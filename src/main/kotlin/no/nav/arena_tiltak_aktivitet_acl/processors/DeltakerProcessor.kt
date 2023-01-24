package no.nav.arena_tiltak_aktivitet_acl.processors

import ArenaOrdsProxyClient
import no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging.Oppfolgingsperiode
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.*
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaDeltakerKafkaMessage
import no.nav.arena_tiltak_aktivitet_acl.exceptions.DependencyNotIngestedException
import no.nav.arena_tiltak_aktivitet_acl.exceptions.IgnoredException
import no.nav.arena_tiltak_aktivitet_acl.exceptions.OppfolgingsperiodeNotFoundException
import no.nav.arena_tiltak_aktivitet_acl.exceptions.OutOfOrderException
import no.nav.arena_tiltak_aktivitet_acl.processors.converters.ArenaDeltakerConverter
import no.nav.arena_tiltak_aktivitet_acl.processors.converters.ArenaDeltakerConverter.toAktivitetStatus
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.GjennomforingRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.PersonSporingDbo
import no.nav.arena_tiltak_aktivitet_acl.services.*
import no.nav.arena_tiltak_aktivitet_acl.utils.SecureLog.secureLog
import no.nav.arena_tiltak_aktivitet_acl.services.OppfolgingsperiodeService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
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

		if (deltaker.regDato.isBefore(AKTIVITETSPLAN_LANSERINGSDATO)) {
			throw IgnoredException("Deltakeren registrert=${deltaker.regDato} opprettet før aktivitetsplan skal ikke håndteres")
		}

		val personIdent = ordsClient.hentFnr(deltaker.personId)
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

		val aktivitetIdExists = arenaIdTranslationService.aktivitetIdExists(deltaker.tiltakdeltakerId, AktivitetKategori.TILTAKSAKTIVITET)

		var maybeOppfolgingsperiode: Oppfolgingsperiode? = null
		if (!aktivitetIdExists) {
			val opprettetTidspunkt = deltaker.regDato
			maybeOppfolgingsperiode = oppfolgingsperiodeService.finnOppfolgingsperiode(personIdent, opprettetTidspunkt)

			val aktivitetStatus = toAktivitetStatus(deltaker.deltakerStatusKode)

			if (maybeOppfolgingsperiode == null) {
				if ((listOf(AktivitetStatus.FULLFORT, AktivitetStatus.AVBRUTT).contains(aktivitetStatus)) || (deltaker.datoTil != null && LocalDate.now().isAfter(deltaker.datoTil))) {
					throw IgnoredException("Avsluttet deltakelse og ingen oppfølgingsperiode, id=${arenaDeltaker.TILTAKDELTAKER_ID} og fodselsnummer=${personIdent}")
				} else {
					throw OppfolgingsperiodeNotFoundException("Pågående deltakelse opprettetTidspunkt=${opprettetTidspunkt}, oppfølgingsperiode ikke startet, id=${arenaDeltaker.TILTAKDELTAKER_ID} og fodselsnummer=${personIdent}")
				}
			}
		}

		val (aktivitetId, nyAktivitet) = arenaIdTranslationService.hentEllerOpprettAktivitetId(deltaker.tiltakdeltakerId, AktivitetKategori.TILTAKSAKTIVITET)

		val aktivitet = ArenaDeltakerConverter
			.convertToTiltaksaktivitet(
				deltaker = deltaker,
				aktivitetId = aktivitetId,
				personIdent = personIdent,
				arrangorNavn = gjennomforing.arrangorNavn,
				gjennomforingNavn = gjennomforing.navn ?: tiltak.navn,
				tiltak = tiltak,
				nyAktivitet = nyAktivitet,
			)

		val kafkaMessage = KafkaMessageDto(
			messageId = UUID.randomUUID(),
			actionType = ActionType.UPSERT_AKTIVITETSKORT_V1,
			aktivitetskort = aktivitet,
			aktivitetskortType = AktivitetskortType.ARENA_TILTAK
		)

		kafkaProducerService.sendTilAktivitetskortTopic(
			aktivitet.id,
			kafkaMessage,
			tiltak.kode,
			deltaker.tiltakdeltakerId.toString(),
			maybeOppfolgingsperiode?.uuid
		)
		arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(deltaker.tiltakdeltakerId))


		aktivitetService.upsert(aktivitet)
		secureLog.info("Melding for deltaker id=$aktivitetId arenaId=${deltaker.tiltakdeltakerId} personId=${deltaker.personId} fnr=$personIdent er sendt")
		log.info("Melding id=${kafkaMessage.messageId} arenaId=${deltaker.tiltakdeltakerId} type=${kafkaMessage.actionType} er sendt")
	}

	//	Alle tiltaksaktiviteter hentes med unntak for tiltak av
	//	administrasjonstypene Institusjonelt tiltak (INST) og Individuelt tiltak (IND) som har deltakerstatus Aktuell (AKTUELL).
	private fun skalIgnoreres(arenaDeltakerStatusKode: String, administrasjonskode: Tiltak.Administrasjonskode): Boolean {
		return arenaDeltakerStatusKode == "AKTUELL"
			&& administrasjonskode in listOf(Tiltak.Administrasjonskode.IND, Tiltak.Administrasjonskode.INST)
	}


}
