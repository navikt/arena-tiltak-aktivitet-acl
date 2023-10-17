package no.nav.arena_tiltak_aktivitet_acl.processors

import no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging.Oppfolgingsperiode
import no.nav.arena_tiltak_aktivitet_acl.domain.db.DeltakerAktivitetMappingDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetskortHeaders
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Tiltak
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.ArenaDeltakerKafkaMessage
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.TiltakDeltakelse
import no.nav.arena_tiltak_aktivitet_acl.exceptions.DependencyNotIngestedException
import no.nav.arena_tiltak_aktivitet_acl.exceptions.IgnoredException
import no.nav.arena_tiltak_aktivitet_acl.exceptions.OppfolgingsperiodeNotFoundException
import no.nav.arena_tiltak_aktivitet_acl.exceptions.OutOfOrderException
import no.nav.arena_tiltak_aktivitet_acl.processors.converters.ArenaDeltakerConverter
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.DeltakerAktivitetMappingRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.GjennomforingRepository
import no.nav.arena_tiltak_aktivitet_acl.services.*
import no.nav.arena_tiltak_aktivitet_acl.services.OppfolgingsperiodeService.Companion.defaultSlakk
import no.nav.arena_tiltak_aktivitet_acl.services.OppfolgingsperiodeService.Companion.tidspunktTidligereEnnRettFoerStartDato
import no.nav.arena_tiltak_aktivitet_acl.utils.SecureLog.secureLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.util.*

@Component
open class DeltakerProcessor(
	private val arenaDataRepository: ArenaDataRepository,
	private val arenaIdArenaIdTilAktivitetskortIdService: ArenaIdTilAktivitetskortIdService,
	private val kafkaProducerService: KafkaProducerService,
	private val gjennomforingRepository: GjennomforingRepository,
	private val aktivitetService: AktivitetService,
	private val tiltakService: TiltakService,
	private val personsporingService: PersonsporingService,
	private val oppfolgingsperiodeService: OppfolgingsperiodeService,
	private val deltakerAktivitetMappingRepository: DeltakerAktivitetMappingRepository
) : ArenaMessageProcessor<ArenaDeltakerKafkaMessage> {

	companion object {
		val AKTIVITETSPLAN_LANSERINGSDATO: LocalDateTime = LocalDateTime.of(2017, Month.DECEMBER, 4, 0,0)
	}

	private val log = LoggerFactory.getLogger(javaClass)

	override fun handleArenaMessage(message: ArenaDeltakerKafkaMessage) {
		val arenaDeltaker = message.getData()
		val arenaGjennomforingId = arenaDeltaker.TILTAKGJENNOMFORING_ID
		val deltakelse = arenaDeltaker.mapTiltakDeltakelse()

		if (message.operationType == Operation.DELETED) {
			throw IgnoredException("Skal ignorere deltakelse med operation type DELETE")
		}
		if (deltakelse.regDato.isBefore(AKTIVITETSPLAN_LANSERINGSDATO)) {
			throw IgnoredException("Deltakeren registrert=${deltakelse.regDato} opprettet før aktivitetsplan skal ikke håndteres")
		}
		val ingestStatus: IngestStatus? = runCatching {
			arenaDataRepository.get(
				message.arenaTableName,
				message.operationType,
				message.operationPosition
			).ingestStatus
		}.getOrNull()

		val hasUnhandled = arenaDataRepository.hasUnhandledDeltakelse(arenaDeltaker.TILTAKDELTAKER_ID)
		val isFirstInQueue = ingestStatus == IngestStatus.RETRY || ingestStatus == IngestStatus.FAILED
		if (hasUnhandled && !isFirstInQueue) throw OutOfOrderException("Venter på at tidligere deltakelse med id=${arenaDeltaker.TILTAKDELTAKER_ID} skal bli håndtert")

		val gjennomforing = gjennomforingRepository.get(arenaGjennomforingId)
			?: throw DependencyNotIngestedException("Venter på at gjennomføring med id=$arenaGjennomforingId skal bli håndtert")

		val tiltak = tiltakService.getByKode(gjennomforing.tiltakKode)
			?: throw DependencyNotIngestedException("Venter på at tiltak med id=${gjennomforing.tiltakKode} skal bli håndtert")

		val personIdent = personsporingService.get(deltakelse.personId, arenaGjennomforingId).fodselsnummer

		/*
		 Hvis oppfølgingsperiode ikke finnes,
		 hopper vi ut her, enten med retry eller ignored, siden handleOppfolgingsperiodeNull kaster exception alltid.
		 Dette er viktig for å ikke opprette ny aktivitetsid før vi faktisk lagrer et aktivitetskort.
		*/
		val oppfolgingsperiodePaaEndringsTidspunkt = getOppfolgingsPeriodeOrThrow(deltakelse, personIdent)
		val endring = utledEndringsType(oppfolgingsperiodePaaEndringsTidspunkt, deltakelse.tiltakdeltakelseId, arenaDeltaker.DELTAKERSTATUSKODE, tiltak.administrasjonskode)
		when (endring) {
			is EndringsType.NyttAktivitetskortByttPeriode  -> {
				secureLog.info("Endring på deltakelse ${deltakelse.tiltakdeltakelseId} på deltakerId ${deltakelse.tiltakdeltakelseId} til ny aktivitetsid ${endring.aktivitetskortId} og oppfølgingsperiode ${oppfolgingsperiodePaaEndringsTidspunkt}. " +
					"Oppretter nytt aktivitetskort for personIdent $personIdent og endrer eksisterende translation entry")
				endring.oppdaterMappingMedNyId(deltakelse.tiltakdeltakelseId)
				arenaIdArenaIdTilAktivitetskortIdService.setCurrentAktivitetskortIdForDeltakerId(deltakelse.tiltakdeltakelseId, endring.aktivitetskortId)
			}
			is EndringsType.NyttAktivitetskort -> {
				arenaIdArenaIdTilAktivitetskortIdService.opprettAktivitetsId(endring.aktivitetskortId, deltakelse.tiltakdeltakelseId, AktivitetKategori.TILTAKSAKTIVITET)
				endring.oppdaterMappingMedNyId(deltakelse.tiltakdeltakelseId)
			}
			is EndringsType.OppdaterAktivitet -> {}
		}

		if (endring.skalIgnoreres) {
			log.info("Deltakeren har status=${arenaDeltaker.DELTAKERSTATUSKODE} og administrasjonskode=${tiltak.administrasjonskode} som ikke skal håndteres")
			arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(deltakelse.tiltakdeltakelseId))
			return
		}

		val aktivitet = ArenaDeltakerConverter
			.convertToTiltaksaktivitet(
				deltaker = deltakelse,
				aktivitetskortId = endring.aktivitetskortId,
				personIdent = personIdent,
				arrangorNavn = gjennomforing.arrangorNavn,
				gjennomforingNavn = gjennomforing.navn ?: "Ukjent navn",
				tiltak = tiltak,
			)
		val aktivitetskortHeaders = AktivitetskortHeaders(
			arenaId = "${KafkaProducerService.TILTAK_ID_PREFIX}${deltakelse.tiltakdeltakelseId}",
			tiltakKode = tiltak.kode,
			oppfolgingsperiode = oppfolgingsperiodePaaEndringsTidspunkt.uuid,
			oppfolgingsSluttDato = oppfolgingsperiodePaaEndringsTidspunkt.sluttDato
		)
		val outgoingMessage = aktivitet.toKafkaMessage()
		aktivitetService.upsert(aktivitet, aktivitetskortHeaders)
		arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(deltakelse.tiltakdeltakelseId))

		secureLog.info("Sender melding for aktivitetskort id=${endring.aktivitetskortId} arenaId=${deltakelse.tiltakdeltakelseId} personId=${deltakelse.personId} fnr=$personIdent")
		log.info("Sender medling messageId=${outgoingMessage.messageId} aktivitetskort id=$endring.aktivitetskortId  arenaId=${deltakelse.tiltakdeltakelseId} type=${outgoingMessage.actionType}")
		kafkaProducerService.sendTilAktivitetskortTopic(
			aktivitet.id,
			outgoingMessage,
			aktivitetskortHeaders
		)
	}

	//	Alle tiltaksaktiviteter hentes med unntak for tiltak av
	//	administrasjonstypene Institusjonelt tiltak (INST) og Individuelt tiltak (IND) som har deltakerstatus Aktuell (AKTUELL).
	private fun skalIgnoreres(arenaDeltakerStatusKode: String, administrasjonskode: Tiltak.Administrasjonskode): Boolean {
		return arenaDeltakerStatusKode == "AKTUELL"
			&& administrasjonskode in listOf(Tiltak.Administrasjonskode.IND, Tiltak.Administrasjonskode.INST)
	}

	private fun handleOppfolgingsperiodeNull(deltaker: TiltakDeltakelse, personIdent: String, tidspunkt: LocalDateTime, tiltakDeltakelseId: DeltakelseId): Nothing {
		secureLog.info("Fant ikke oppfølgingsperiode for personIdent=$personIdent")
		val aktivitetStatus = ArenaDeltakerConverter.toAktivitetStatus(deltaker.deltakerStatusKode)
		val erFerdig = deltaker.datoTil?.isBefore(LocalDate.now()) ?: false
		when {
			aktivitetStatus.erAvsluttet() || erFerdig ->
				throw IgnoredException("Avsluttet deltakelse og ingen oppfølgingsperiode, id=${tiltakDeltakelseId.value}")
			tidspunktTidligereEnnRettFoerStartDato(tidspunkt, LocalDateTime.now(), defaultSlakk) ->
				throw IgnoredException("Opprettet for mer enn $defaultSlakk siden og ingen oppfølgingsperiode, id=${tiltakDeltakelseId.value}")
			else -> throw OppfolgingsperiodeNotFoundException("Deltakelse endret tidspunkt=${tidspunkt}, Finner ingen passende oppfølgingsperiode, id=${tiltakDeltakelseId.value}")
		}
	}

	private fun getOppfolgingsPeriodeOrThrow(deltaker: TiltakDeltakelse, personIdent: String): Oppfolgingsperiode {
		return deltaker.modDato?.let { modDato -> oppfolgingsperiodeService.finnOppfolgingsperiode(personIdent, modDato) }
			?: oppfolgingsperiodeService.finnOppfolgingsperiode(personIdent, deltaker.regDato)
				.also { log.info("arenaId: ${deltaker.tiltakdeltakelseId} Fant ikke oppfolgingsperiode på modDato, bruker fallback til regDato") }
			?: handleOppfolgingsperiodeNull(deltaker, personIdent, deltaker.modDato ?: deltaker.regDato, deltaker.tiltakdeltakelseId)
	}

	private fun utledEndringsType(oppfolgingsperiode: Oppfolgingsperiode, deltakelseId: DeltakelseId, deltakerStatusKode: String, administrasjonskode: Tiltak.Administrasjonskode): EndringsType {
		val skalIgnoreres = skalIgnoreres(deltakerStatusKode, administrasjonskode)
		val oppfolgingsperiodeTilAktivitetskortId = deltakerAktivitetMappingRepository.get(deltakelseId, AktivitetKategori.TILTAKSAKTIVITET)
		val eksisterendeAktivitetsId = oppfolgingsperiodeTilAktivitetskortId
			.firstOrNull { it.oppfolgingsperiodeUuid == oppfolgingsperiode.uuid }?.aktivitetId
		return when {
			// Har tidligere deltakelse på samme oppfolgingsperiode
			eksisterendeAktivitetsId != null -> EndringsType.OppdaterAktivitet(eksisterendeAktivitetsId, skalIgnoreres)
			// Har ingen tidligere aktivitetskort
			oppfolgingsperiodeTilAktivitetskortId.isEmpty() -> EndringsType.NyttAktivitetskort(oppfolgingsperiode, skalIgnoreres)
			// Har tidligere deltakelse men ikke på samme oppfølgingsperiode
			else -> EndringsType.NyttAktivitetskortByttPeriode(oppfolgingsperiode, skalIgnoreres)
		}
	}

	fun EndringsType.oppdaterMappingMedNyId(deltakelseId: DeltakelseId) {
		when (this) {
			is EndringsType.NyttAktivitetskort ->  this.oppfolgingsperiode
			is EndringsType.NyttAktivitetskortByttPeriode -> this.oppfolgingsperiode
			is EndringsType.OppdaterAktivitet -> null
		}?.let {
			deltakerAktivitetMappingRepository.insert(
				DeltakerAktivitetMappingDbo(
				deltakelseId = deltakelseId,
				aktivitetId = this.aktivitetskortId,
				aktivitetKategori = AktivitetKategori.TILTAKSAKTIVITET,
				oppfolgingsperiodeUuid = it.uuid)
			)
		}
	}
}

sealed class EndringsType(val aktivitetskortId: UUID, val skalIgnoreres: Boolean) {
	class OppdaterAktivitet(aktivitetskortId: UUID, skalIgnoreres: Boolean): EndringsType(aktivitetskortId, skalIgnoreres)
	class NyttAktivitetskort(val oppfolgingsperiode: Oppfolgingsperiode, skalIgnoreres: Boolean): EndringsType(UUID.randomUUID(), skalIgnoreres)
	class NyttAktivitetskortByttPeriode(val oppfolgingsperiode: Oppfolgingsperiode, skalIgnoreres: Boolean): EndringsType(UUID.randomUUID(), skalIgnoreres)
}



