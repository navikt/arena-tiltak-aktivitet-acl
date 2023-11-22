package no.nav.arena_tiltak_aktivitet_acl.processors

import no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging.Oppfolgingsperiode
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetskortHeaders
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Tiltak
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.ArenaDeltakerKafkaMessage
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.TiltakDeltakelse
import no.nav.arena_tiltak_aktivitet_acl.exceptions.*
import no.nav.arena_tiltak_aktivitet_acl.processors.converters.ArenaDeltakerConverter
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.GjennomforingRepository
import no.nav.arena_tiltak_aktivitet_acl.services.*
import no.nav.arena_tiltak_aktivitet_acl.services.OppfolgingsperiodeService.Companion.defaultSlakk
import no.nav.arena_tiltak_aktivitet_acl.services.OppfolgingsperiodeService.Companion.tidspunktTidligereEnnRettFoerStartDato
import no.nav.arena_tiltak_aktivitet_acl.utils.SecureLog.secureLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.util.*

@Component
open class DeltakerProcessor(
	private val arenaDataRepository: ArenaDataRepository,
	private val kafkaProducerService: KafkaProducerService,
	private val gjennomforingRepository: GjennomforingRepository,
	private val aktivitetService: AktivitetService,
	private val tiltakService: TiltakService,
	private val personsporingService: PersonsporingService,
	private val oppfolgingsperiodeService: OppfolgingsperiodeService,
	private val aktivitetskortIdService: AktivitetskortIdService
) : ArenaMessageProcessor<ArenaDeltakerKafkaMessage> {

	companion object {
		val AKTIVITETSPLAN_LANSERINGSDATO: LocalDateTime = LocalDateTime.of(2017, Month.DECEMBER, 4, 0,0)
	}

	private val log = LoggerFactory.getLogger(javaClass)

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	override fun handleArenaMessage(message: ArenaDeltakerKafkaMessage) {
		val arenaDeltaker = message.getData()
		val arenaGjennomforingId = arenaDeltaker.TILTAKGJENNOMFORING_ID
		val deltakelse = arenaDeltaker.mapTiltakDeltakelse()

		if (message.operationType == Operation.DELETED) {
			throw IgnoredException("Skal ignorere deltakelse med operation type DELETE")
		}

		var sjekkOmBrukerVarUnderOppfolgingVedAktivitetsplanLaunch = false
		if (deltakelse.regDato.isBefore(AKTIVITETSPLAN_LANSERINGSDATO)) {
			// Hvis deltakelsen er opprettet før aktivitetsplan lanseringsdato,
			// _men_ datoTil er etter aktivitetsplan lanseringsdato,
			// _og_ bruker hadde en aktiv oppfølgingsperiode ved aktivitetsplan lanseringsdato
			// så skal vi opprette aktivitetskort
			if (deltakelse.datoTil?.isAfter(AKTIVITETSPLAN_LANSERINGSDATO.toLocalDate()) == true) {
				sjekkOmBrukerVarUnderOppfolgingVedAktivitetsplanLaunch = true
			} else throw IgnoredException("Deltakeren registrert=${deltakelse.regDato} opprettet før aktivitetsplan skal ikke håndteres")
		}
		val ingestStatus: IngestStatus? = runCatching {
			arenaDataRepository.get(
				message.arenaTableName,
				message.operationType,
				message.operationPosition
			).ingestStatus
		}.getOrNull()

		val hasNewerHandledMessages = arenaDataRepository.hasHandledDeltakelseWithLaterPos(DeltakelseId(arenaDeltaker.TILTAKDELTAKER_ID), message.operationPosition)
		if (hasNewerHandledMessages) throw OlderThanCurrentStateException("Har behandlet nyere meldinger på id=${arenaDeltaker.TILTAKDELTAKER_ID} allerede. Hopper over melding")

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
		*/
		val periodeMatch =
			if (sjekkOmBrukerVarUnderOppfolgingVedAktivitetsplanLaunch) {
				val oppfolgingsperiodeVedAktivitetsplanLansering = oppfolgingsperiodeService.finnOppfolgingsperiode(personIdent, AKTIVITETSPLAN_LANSERINGSDATO)
				when (oppfolgingsperiodeVedAktivitetsplanLansering) {
					is FinnOppfolgingResult.FunnetPeriodeResult -> oppfolgingsperiodeVedAktivitetsplanLansering
					is FinnOppfolgingResult.IngenPeriodeResult -> throw IgnoredException("Deltakelse aktiv ved aktivitetsplan lansering, men bruker ikke under oppfølging på det tidspunktet.")
				}
			} else getOppfolgingsPeriodeOrThrow(deltakelse, personIdent)
		val endring = utledEndringsType(periodeMatch, deltakelse.tiltakdeltakelseId, arenaDeltaker.DELTAKERSTATUSKODE, tiltak.administrasjonskode)
		when (endring) {
			is EndringsType.NyttAktivitetskortByttPeriode  -> {
				secureLog.info("Endring på deltakelse ${deltakelse.tiltakdeltakelseId} på deltakerId ${deltakelse.tiltakdeltakelseId} til ny aktivitetsid ${endring.aktivitetskortId} og oppfølgingsperiode ${periodeMatch.oppfolgingsperiode.uuid}. " +
					"Oppretter nytt aktivitetskort for personIdent $personIdent og endrer eksisterende translation entry")
				syncOppfolgingsperioder(deltakelse.tiltakdeltakelseId, periodeMatch.allePerioder)
			}
			is EndringsType.NyttAktivitetskort -> {}
			is EndringsType.OppdaterAktivitet -> {
				log.info("Patcher oppfølgingsperiode sluttdato for aktivitet deltakerId:${deltakelse.tiltakdeltakelseId}")
				syncOppfolgingsperioder(deltakelse.tiltakdeltakelseId, periodeMatch.allePerioder)
			}
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
			oppfolgingsperiode = periodeMatch.oppfolgingsperiode.uuid,
			oppfolgingsSluttDato = periodeMatch.oppfolgingsperiode.sluttDato
		)
		aktivitetService.upsert(aktivitet, aktivitetskortHeaders, deltakelse.tiltakdeltakelseId)

		if (endring.skalIgnoreres) {
			log.info("Deltakeren har status=${arenaDeltaker.DELTAKERSTATUSKODE} og administrasjonskode=${tiltak.administrasjonskode} som ikke skal håndteres")
			arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(deltakelse.tiltakdeltakelseId, "foreløpig ignorert"))
			return
		}

		arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(deltakelse.tiltakdeltakelseId))
		val outgoingMessage = aktivitet.toKafkaMessage()
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

	private fun getOppfolgingsPeriodeOrThrow(deltaker: TiltakDeltakelse, personIdent: String): FinnOppfolgingResult.FunnetPeriodeResult {
		val funnetPeriode = deltaker.modDato
			?.let { modDato -> oppfolgingsperiodeService.finnOppfolgingsperiode(personIdent, modDato) }
			?: oppfolgingsperiodeService.finnOppfolgingsperiode(personIdent, deltaker.regDato)
				.also { log.info("arenaId: ${deltaker.tiltakdeltakelseId} Fant ikke oppfolgingsperiode på modDato, bruker fallback til regDato") }
		return when (funnetPeriode) {
			is FinnOppfolgingResult.FunnetPeriodeResult -> funnetPeriode
			is FinnOppfolgingResult.IngenPeriodeResult -> handleOppfolgingsperiodeNull(deltaker, personIdent, deltaker.modDato ?: deltaker.regDato, deltaker.tiltakdeltakelseId)
		}
	}

	private fun utledEndringsType(
		periodeMatch: FinnOppfolgingResult.FunnetPeriodeResult,
		deltakelseId: DeltakelseId,
		deltakerStatusKode: String,
		administrasjonskode: Tiltak.Administrasjonskode,
	): EndringsType {
		val skalIgnoreres = skalIgnoreres(deltakerStatusKode, administrasjonskode)
		val oppfolgingsperiodeTilAktivitetskortId = aktivitetService.getAllBy(deltakelseId, AktivitetKategori.TILTAKSAKTIVITET)
		val eksisterendeAktivitetsId = oppfolgingsperiodeTilAktivitetskortId
			.firstOrNull { it.oppfolgingsPeriode == periodeMatch.oppfolgingsperiode.uuid }?.id
		return when {
			// Har tidligere deltakelse på samme oppfolgingsperiode
			eksisterendeAktivitetsId != null -> EndringsType.OppdaterAktivitet(eksisterendeAktivitetsId, skalIgnoreres)
			// Har ingen tidligere aktivitetskort
			oppfolgingsperiodeTilAktivitetskortId.isEmpty() -> EndringsType.NyttAktivitetskort(getAkivitetskortId(deltakelseId), periodeMatch.oppfolgingsperiode, skalIgnoreres)
			// Har tidligere deltakelse men ikke på samme oppfølgingsperiode
			else -> {
				EndringsType.NyttAktivitetskortByttPeriode(periodeMatch.oppfolgingsperiode, skalIgnoreres)
			}
		}
	}

	fun getAkivitetskortId(deltakelseId: DeltakelseId): UUID {
		return aktivitetskortIdService.getOrCreate(deltakelseId, AktivitetKategori.TILTAKSAKTIVITET)
	}

	fun syncOppfolgingsperioder(deltakelseId: DeltakelseId, oppfolginsperioder: List<Oppfolgingsperiode>) {
		aktivitetService.closeClosedPerioder(deltakelseId, AktivitetKategori.TILTAKSAKTIVITET, oppfolginsperioder)
	}
}

sealed class EndringsType(val aktivitetskortId: UUID, val skalIgnoreres: Boolean) {
	class OppdaterAktivitet(aktivitetskortId: UUID, skalIgnoreres: Boolean): EndringsType(aktivitetskortId, skalIgnoreres)
	class NyttAktivitetskort(aktivitetskortId:UUID, val oppfolgingsperiode: Oppfolgingsperiode, skalIgnoreres: Boolean): EndringsType(aktivitetskortId, skalIgnoreres)
	class NyttAktivitetskortByttPeriode(val oppfolgingsperiode: Oppfolgingsperiode, skalIgnoreres: Boolean): EndringsType(UUID.randomUUID(), skalIgnoreres)
}



