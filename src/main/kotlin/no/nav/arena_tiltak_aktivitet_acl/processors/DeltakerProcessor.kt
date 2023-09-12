package no.nav.arena_tiltak_aktivitet_acl.processors

import ArenaOrdsProxyClient
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.*
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.ArenaDeltakerKafkaMessage
import no.nav.arena_tiltak_aktivitet_acl.exceptions.DependencyNotIngestedException
import no.nav.arena_tiltak_aktivitet_acl.exceptions.IgnoredException
import no.nav.arena_tiltak_aktivitet_acl.exceptions.OppfolgingsperiodeNotFoundException
import no.nav.arena_tiltak_aktivitet_acl.exceptions.OutOfOrderException
import no.nav.arena_tiltak_aktivitet_acl.processors.converters.ArenaDeltakerConverter
import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetDbo
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.GjennomforingRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.PersonSporingDbo
import no.nav.arena_tiltak_aktivitet_acl.services.*
import no.nav.arena_tiltak_aktivitet_acl.services.OppfolgingsperiodeService.Companion.merEnnEnUkeMellom
import no.nav.arena_tiltak_aktivitet_acl.utils.SecureLog.secureLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZonedDateTime
import java.util.*

@Component
open class DeltakerProcessor(
	private val arenaDataRepository: ArenaDataRepository,
	private val arenaIdTranslationService: TranslationService,
	private val ordsClient: ArenaOrdsProxyClient,
	private val kafkaProducerService: KafkaProducerService,
	private val gjennomforingRepository: GjennomforingRepository,
	private val aktivitetService: AktivitetService,
	private val tiltakService: TiltakService,
	private val personsporingService: PersonsporingService,
	private val oppfolgingsperiodeService: OppfolgingsperiodeService
) : ArenaMessageProcessor<ArenaDeltakerKafkaMessage> {

	companion object {
		val AKTIVITETSPLAN_LANSERINGSDATO: LocalDateTime = LocalDateTime.of(2017, Month.DECEMBER, 4, 0,0)
	}

	private val log = LoggerFactory.getLogger(javaClass)

	override fun handleArenaMessage(message: ArenaDeltakerKafkaMessage) {
		val arenaDeltaker = message.getData()
		val arenaGjennomforingId = arenaDeltaker.TILTAKGJENNOMFORING_ID
		val deltaker = arenaDeltaker.mapTiltakDeltaker()

		if (message.operationType == Operation.DELETED) {
			throw IgnoredException("Skal ignorere deltakelse med operation type DELETE")
		}
		if (deltaker.regDato.isBefore(AKTIVITETSPLAN_LANSERINGSDATO)) {
			throw IgnoredException("Deltakeren registrert=${deltaker.regDato} opprettet før aktivitetsplan skal ikke håndteres")
		}

		// Translation opprettes ikke før denne er kjørt.
		val aktivitetId = arenaIdTranslationService.hentEllerOpprettAktivitetId(deltaker.tiltakdeltakerId, AktivitetKategori.TILTAKSAKTIVITET)
		val personIdent = personsporingService.get(deltaker.personId, arenaGjennomforingId)?.fodselsnummer ?: ordsClient.hentFnr(deltaker.personId)
			?: throw IllegalStateException("Expected person with personId=${deltaker.personId} to exist")
		personsporingService.upsert(PersonSporingDbo(personIdent = deltaker.personId, fodselsnummer = personIdent, tiltakgjennomforingId = arenaGjennomforingId))

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

		val aktivitetskort = aktivitetService.get(aktivitetId)
		val erNyAktivitet = aktivitetskort != null

		val fallbackGjennomforingNavn = "Ukjent navn"

		val aktivitet = ArenaDeltakerConverter
			.convertToTiltaksaktivitet(
				deltaker = deltaker,
				aktivitetId = aktivitetId,
				personIdent = personIdent,
				arrangorNavn = gjennomforing.arrangorNavn,
				gjennomforingNavn = gjennomforing.navn ?: fallbackGjennomforingNavn,
				tiltak = tiltak,
				erNyAktivitet = erNyAktivitet,
			)

		val oppfolgingsperiode = aktivitetskort?.oppfolgingsPeriode()
			?: getOppfolgingsPeriodeOrThrow(aktivitet, deltaker.regDato, deltaker.tiltakdeltakerId)

		val aktivitetskortHeaders = AktivitetskortHeaders(
			arenaId = KafkaProducerService.TILTAK_ID_PREFIX + deltaker.tiltakdeltakerId.toString(),
			tiltakKode = tiltak.kode,
			oppfolgingsperiode = oppfolgingsperiode.id,
			oppfolgingsSluttDato = oppfolgingsperiode.oppfolgingsSluttDato
		)
		val outgoingMessage = aktivitet.toKafkaMessage()
		kafkaProducerService.sendTilAktivitetskortTopic(
			aktivitet.id,
			outgoingMessage,
			aktivitetskortHeaders
		)
		secureLog.info("Melding for deltaker id=$aktivitetId arenaId=${deltaker.tiltakdeltakerId} personId=${deltaker.personId} fnr=$personIdent er sendt")
		log.info("Melding id=${outgoingMessage.messageId} arenaId=${deltaker.tiltakdeltakerId} type=${outgoingMessage.actionType} er sendt")
		aktivitetService.upsert(aktivitet, aktivitetskortHeaders)
		arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(deltaker.tiltakdeltakerId))
	}

	//	Alle tiltaksaktiviteter hentes med unntak for tiltak av
	//	administrasjonstypene Institusjonelt tiltak (INST) og Individuelt tiltak (IND) som har deltakerstatus Aktuell (AKTUELL).
	private fun skalIgnoreres(arenaDeltakerStatusKode: String, administrasjonskode: Tiltak.Administrasjonskode): Boolean {
		return arenaDeltakerStatusKode == "AKTUELL"
			&& administrasjonskode in listOf(Tiltak.Administrasjonskode.IND, Tiltak.Administrasjonskode.INST)
	}

	data class AktivitetskortOppfolgingsperiode(
		val id: UUID,
		val oppfolgingsSluttDato: ZonedDateTime?
	)
	private fun getOppfolgingsPeriodeOrThrow(aktivitet: Aktivitetskort, opprettetTidspunkt: LocalDateTime, tiltakDeltakerId: Long): AktivitetskortOppfolgingsperiode {
		val personIdent = aktivitet.personIdent
		val oppfolgingsperiode = oppfolgingsperiodeService.finnOppfolgingsperiode(personIdent, opprettetTidspunkt)
			?.let { AktivitetskortOppfolgingsperiode(it.uuid , it.sluttDato) }
		if (oppfolgingsperiode == null) {
			secureLog.info("Fant ikke oppfølgingsperiode for personIdent=${personIdent}")
			val aktivitetStatus = aktivitet.aktivitetStatus
			val erFerdig = aktivitet.sluttDato?.isBefore(LocalDate.now()) ?: false
			when {
				aktivitetStatus.erAvsluttet() || erFerdig ->
					throw IgnoredException("Avsluttet deltakelse og ingen oppfølgingsperiode, id=${tiltakDeltakerId}")
				merEnnEnUkeMellom(opprettetTidspunkt, LocalDateTime.now()) ->
					throw IgnoredException("Opprettet for over 1 uke siden og ingen oppfølgingsperiode, id=${tiltakDeltakerId}")
				else -> throw OppfolgingsperiodeNotFoundException("Pågående deltakelse opprettetTidspunkt=${opprettetTidspunkt}, oppfølgingsperiode ikke startet/oppfolgingsperiode eldre enn en uke, id=${tiltakDeltakerId}")
			}
		} else {
			return oppfolgingsperiode
		}
	}
}

fun AktivitetDbo.oppfolgingsPeriode() = this.oppfolgingsperiodeUUID?.let {
	DeltakerProcessor.AktivitetskortOppfolgingsperiode(it, this.oppfolgingsSluttTidspunkt)
}


