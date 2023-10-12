package no.nav.arena_tiltak_aktivitet_acl.processors

import no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging.Oppfolgingsperiode
import no.nav.arena_tiltak_aktivitet_acl.domain.db.DeltakerAktivitetMappingDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.*
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
		val deltaker = arenaDeltaker.mapTiltakDeltakelse()

		if (message.operationType == Operation.DELETED) {
			throw IgnoredException("Skal ignorere deltakelse med operation type DELETE")
		}
		if (deltaker.regDato.isBefore(AKTIVITETSPLAN_LANSERINGSDATO)) {
			throw IgnoredException("Deltakeren registrert=${deltaker.regDato} opprettet før aktivitetsplan skal ikke håndteres")
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

		if (skalIgnoreres(arenaDeltaker.DELTAKERSTATUSKODE, tiltak.administrasjonskode)) {
			throw IgnoredException("Deltakeren har status=${arenaDeltaker.DELTAKERSTATUSKODE} og administrasjonskode=${tiltak.administrasjonskode} som ikke skal håndteres")
		}

		val personIdent = personsporingService.get(deltaker.personId, arenaGjennomforingId).fodselsnummer

		/*
		 Hvis oppfølgingsperiode ikke finnes,
		 hopper vi ut her, enten med retry eller ignored, siden handleOppfolgingsperiodeNull kaster exception alltid.
		 Dette er viktig for å ikke opprette ny aktivitetsid før vi faktisk lagrer et aktivitetskort.
		*/
		val oppfolgingsperiodePaaEndringsTidspunkt = getOppfolgingsPeriodeOrThrow(deltaker, personIdent, deltaker.modDato ?: deltaker.regDato, deltaker.tiltakdeltakelseId)
		val endring = utledEndringsType(oppfolgingsperiodePaaEndringsTidspunkt, deltaker.tiltakdeltakelseId)
		when (endring) {
			is EndringsType.NyttAktivitetskortByttPeriode -> {
				secureLog.info("Endring på deltakelse ${deltaker.tiltakdeltakelseId} på deltakerId ${deltaker.tiltakdeltakelseId} til ny aktivitetsid ${endring.aktivitetskortId} og oppfølgingsperiode ${oppfolgingsperiodePaaEndringsTidspunkt}. " +
					"Oppretter nytt aktivitetskort for personIdent $personIdent og endrer eksisterende translation entry")
				endring.oppdaterMappingMedNyId(deltaker.tiltakdeltakelseId)
				arenaIdArenaIdTilAktivitetskortIdService.setCurrentAktivitetskortIdForDeltakerId(deltaker.tiltakdeltakelseId, endring.aktivitetskortId)
			}
			is EndringsType.NyttAktivitetskort -> {
				arenaIdArenaIdTilAktivitetskortIdService.opprettAktivitetsId(endring.aktivitetskortId, deltaker.tiltakdeltakelseId, AktivitetKategori.TILTAKSAKTIVITET)
				endring.oppdaterMappingMedNyId(deltaker.tiltakdeltakelseId)
			}
			is EndringsType.OppdaterAktivitet -> {}
		}

		val aktivitet = ArenaDeltakerConverter
			.convertToTiltaksaktivitet(
				deltaker = deltaker,
				aktivitetskortId = endring.aktivitetskortId,
				personIdent = personIdent,
				arrangorNavn = gjennomforing.arrangorNavn,
				gjennomforingNavn = gjennomforing.navn ?: "Ukjent navn",
				tiltak = tiltak,
			)
		val aktivitetskortHeaders = AktivitetskortHeaders(
			arenaId = "${KafkaProducerService.TILTAK_ID_PREFIX}${deltaker.tiltakdeltakelseId}",
			tiltakKode = tiltak.kode,
			oppfolgingsperiode = oppfolgingsperiodePaaEndringsTidspunkt.uuid,
			oppfolgingsSluttDato = oppfolgingsperiodePaaEndringsTidspunkt.sluttDato
		)
		val outgoingMessage = aktivitet.toKafkaMessage()
		kafkaProducerService.sendTilAktivitetskortTopic(
			aktivitet.id,
			outgoingMessage,
			aktivitetskortHeaders
		)
		secureLog.info("Melding for aktivitetskort id=${endring.aktivitetskortId} arenaId=${deltaker.tiltakdeltakelseId} personId=${deltaker.personId} fnr=$personIdent er sendt")
		log.info("Melding id=${outgoingMessage.messageId} aktivitetskort id=$endring.aktivitetskortId  arenaId=${deltaker.tiltakdeltakelseId} type=${outgoingMessage.actionType} er sendt")
		aktivitetService.upsert(aktivitet, aktivitetskortHeaders)
		arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(deltaker.tiltakdeltakelseId))
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

	private fun getOppfolgingsPeriodeOrThrow(deltaker: TiltakDeltakelse, personIdent: String, tidspunkt: LocalDateTime, tiltakDeltakelseId: DeltakelseId): Oppfolgingsperiode {
		return oppfolgingsperiodeService.finnOppfolgingsperiode(personIdent, tidspunkt)
			?: handleOppfolgingsperiodeNull(deltaker, personIdent, tidspunkt, tiltakDeltakelseId)
	}

	private fun utledEndringsType(oppfolgingsperiode: Oppfolgingsperiode, deltakelseId: DeltakelseId): EndringsType {
		val oppfolgingsperiodeTilAktivitetskortId = deltakerAktivitetMappingRepository.get(deltakelseId, AktivitetKategori.TILTAKSAKTIVITET)
		val aktivitetId = oppfolgingsperiodeTilAktivitetskortId
			.firstOrNull { it.oppfolgingsperiodeUuid == oppfolgingsperiode.uuid }?.aktivitetId
		return when {
			// Har tidligere deltakelse på samme oppfolgingsperiode
			aktivitetId != null -> EndringsType.OppdaterAktivitet(aktivitetId)
			// Har ingen tidligere aktivitetskort
			oppfolgingsperiodeTilAktivitetskortId.isEmpty() -> EndringsType.NyttAktivitetskort(oppfolgingsperiode)
			// Har tidligere deltakelse men ikke på samme oppfølgingsperiode
			else -> EndringsType.NyttAktivitetskortByttPeriode(oppfolgingsperiode)
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

sealed class EndringsType(val aktivitetskortId: UUID) {
	class OppdaterAktivitet(aktivitetskortId: UUID): EndringsType(aktivitetskortId)
	class NyttAktivitetskort(val oppfolgingsperiode: Oppfolgingsperiode): EndringsType(UUID.randomUUID())
	class NyttAktivitetskortByttPeriode(val oppfolgingsperiode: Oppfolgingsperiode): EndringsType(UUID.randomUUID())
}



