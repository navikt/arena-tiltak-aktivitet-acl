package no.nav.arena_tiltak_aktivitet_acl.processors

import ArenaOrdsProxyClient
import no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging.Oppfolgingsperiode
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetskortHeaders
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaGruppeTiltakKafkaMessage
import no.nav.arena_tiltak_aktivitet_acl.exceptions.IgnoredException
import no.nav.arena_tiltak_aktivitet_acl.exceptions.OutOfOrderException
import no.nav.arena_tiltak_aktivitet_acl.processors.converters.ArenaGruppeTiltakConverter
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.services.*
import no.nav.arena_tiltak_aktivitet_acl.utils.ARENA_GRUPPETILTAK_TABLE_NAME
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.Month

@Component
open class GruppeTiltakProcessor(
	private val arenaDataRepository: ArenaDataRepository,
	private val arenaIdTranslationService: TranslationService,
	private val ordsClient: ArenaOrdsProxyClient,
	private val kafkaProducerService: KafkaProducerService,
	private val aktivitetService: AktivitetService,
	private val tiltakService: TiltakService,
	private val oppfolgingsperiodeService: OppfolgingsperiodeService
) : ArenaMessageProcessor<ArenaGruppeTiltakKafkaMessage> {

	companion object {
		val AKTIVITETSPLAN_LANSERINGSDATO: LocalDateTime = LocalDateTime.of(2017, Month.DECEMBER, 4, 0,0)
	}

	private val log = LoggerFactory.getLogger(javaClass)

	override fun handleArenaMessage(message: ArenaGruppeTiltakKafkaMessage) {
		val arenaGruppeTiltak = message.getData()

		val gruppeTiltak = arenaGruppeTiltak.mapGruppeTiltak()

		if (message.operationType == Operation.DELETED) {

		}

		if (gruppeTiltak.opprettetTid.isBefore(AKTIVITETSPLAN_LANSERINGSDATO)) {
			throw IgnoredException("Gruppetiltak registrert=${gruppeTiltak.opprettetTid} opprettet før aktivitetsplan skal ikke håndteres")
		}

		val personIdent = arenaGruppeTiltak.PERSONIDENT // valider? fallback til arena personid-translation?

		val ingestStatus: IngestStatus? = runCatching {
			arenaDataRepository.get(
				message.arenaTableName,
				message.operationType,
				message.operationPosition
			).ingestStatus
		}.getOrNull()

		val hasUnhandled = arenaDataRepository.hasUnhandledRow(arenaGruppeTiltak.AKTIVITET_ID, ARENA_GRUPPETILTAK_TABLE_NAME)
		val isFirstInQueue = ingestStatus == IngestStatus.RETRY || ingestStatus == IngestStatus.FAILED
		if (hasUnhandled && !isFirstInQueue) throw OutOfOrderException("Venter på at tidligere gruppetiltak med id=${arenaGruppeTiltak.AKTIVITET_ID} skal bli håndtert")


		val aktivitetIdExists = arenaIdTranslationService.aktivitetIdExists(arenaGruppeTiltak.AKTIVITET_ID, AktivitetKategori.GRUPPEAKTIVITET)

		var maybeOppfolgingsperiode: Oppfolgingsperiode? = null
		if (!aktivitetIdExists) {
			val opprettetTidspunkt = gruppeTiltak.opprettetTid
			maybeOppfolgingsperiode = oppfolgingsperiodeService.finnOppfolgingsperiode(personIdent, opprettetTidspunkt)

//			val aktivitetStatus = toAktivitetStatus(deltaker.deltakerStatusKode)

//			if (maybeOppfolgingsperiode == null) {
//				log.info("Fant ikke oppfølgingsperiode for arenaId=${deltaker.tiltakdeltakerId}")
//				if ((listOf(AktivitetStatus.FULLFORT, AktivitetStatus.AVBRUTT).contains(aktivitetStatus)) || (deltaker.datoTil != null && LocalDate.now().isAfter(deltaker.datoTil))) {
//					throw IgnoredException("Avsluttet deltakelse og ingen oppfølgingsperiode, id=${arenaDeltaker.TILTAKDELTAKER_ID} og fodselsnummer=${personIdent}")
//				} else {
//					throw OppfolgingsperiodeNotFoundException("Pågående deltakelse opprettetTidspunkt=${opprettetTidspunkt}, oppfølgingsperiode ikke startet/oppfolgingsperiode eldre enn en uke, id=${arenaDeltaker.TILTAKDELTAKER_ID} og fodselsnummer=${personIdent}")
//				}
//			}
		}

		val (aktivitetId, nyAktivitet) = arenaIdTranslationService.hentEllerOpprettAktivitetId(arenaGruppeTiltak.AKTIVITET_ID, AktivitetKategori.GRUPPEAKTIVITET)

		val aktivitet = ArenaGruppeTiltakConverter
			.convertToTiltaksaktivitet(
				aktivitetId = aktivitetId,
				personIdent = personIdent,
				nyAktivitet = nyAktivitet,
				gruppeTiltak = gruppeTiltak,
				kafkaOperation = message.operationType
			)

		val aktivitetskortHeaders = AktivitetskortHeaders(
			arenaId = KafkaProducerService.GRUPPE_TILTAK_ID_PREFIX + arenaGruppeTiltak.AKTIVITET_ID.toString(),
			tiltakKode = arenaGruppeTiltak.AKTIVITETSTYPE,
			oppfolgingsperiode = maybeOppfolgingsperiode?.uuid,
			historisk = maybeOppfolgingsperiode?.let { it.sluttDato != null }
		)

//		val kafkaMessage = KafkaMessageDto(
//			messageId = UUID.randomUUID(),
//			actionType = ActionType.UPSERT_AKTIVITETSKORT_V1,
//			aktivitetskort = aktivitet,
//			aktivitetskortType = AktivitetskortType.ARENA_TILTAK
//		)
//
//		kafkaProducerService.sendTilAktivitetskortTopic(
//			aktivitet.id,
//			kafkaMessage,
//			aktivitetskortHeaders
//		)

		arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(arenaGruppeTiltak.AKTIVITET_ID))

		aktivitetService.upsert(aktivitet, aktivitetskortHeaders)
//		secureLog.info("Melding for deltaker id=$aktivitetId arenaId=${deltaker.tiltakdeltakerId} personId=${deltaker.personId} fnr=$personIdent er sendt")
//		log.info("Melding id=${kafkaMessage.messageId} arenaId=${deltaker.tiltakdeltakerId} type=${kafkaMessage.actionType} er sendt")
	}

}
