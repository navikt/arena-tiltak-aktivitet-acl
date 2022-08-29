package no.nav.arena_tiltak_aktivitet_acl.processors

import ArenaOrdsProxyClient
import no.nav.arena_tiltak_aktivitet_acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Aktivitet
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.KafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.PayloadType
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaDeltakerKafkaMessage
import no.nav.arena_tiltak_aktivitet_acl.exceptions.DependencyNotIngestedException
import no.nav.arena_tiltak_aktivitet_acl.exceptions.IgnoredException
import no.nav.arena_tiltak_aktivitet_acl.metrics.DeltakerMetricHandler
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.GjennomforingRepository
import no.nav.arena_tiltak_aktivitet_acl.services.ArenaDataIdTranslationService
import no.nav.arena_tiltak_aktivitet_acl.services.KafkaProducerService
import no.nav.arena_tiltak_aktivitet_acl.utils.SecureLog.secureLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
open class DeltakerProcessor(
	private val arenaDataRepository: ArenaDataRepository,
	private val arenaDataIdTranslationService: ArenaDataIdTranslationService,
	private val ordsClient: ArenaOrdsProxyClient,
	private val metrics: DeltakerMetricHandler,
	private val kafkaProducerService: KafkaProducerService,
	private val gjennomforingRepository: GjennomforingRepository
) : ArenaMessageProcessor<ArenaDeltakerKafkaMessage> {

	private val log = LoggerFactory.getLogger(javaClass)

	override fun handleArenaMessage(message: ArenaDeltakerKafkaMessage) {
		val arenaDeltaker = message.getData()
		val arenaGjennomforingId = arenaDeltaker.TILTAKGJENNOMFORING_ID

		if (harStatusSomSkalIgnoreres(arenaDeltaker.DELTAKERSTATUSKODE)) {
			throw IgnoredException("Deltakeren har status=${arenaDeltaker.DELTAKERSTATUSKODE} som ikke skal håndteres")
		}

		val tiltakDeltaker = arenaDeltaker.mapTiltakDeltaker()

		val gjennomforing = gjennomforingRepository.get(arenaGjennomforingId)
			?: throw DependencyNotIngestedException("Venter på at gjennomføring med id=$arenaGjennomforingId skal bli håndtert")

		val personIdent = ordsClient.hentFnr(tiltakDeltaker.personId)
			?: throw IllegalStateException("Expected person with personId=${tiltakDeltaker.personId} to exist")

		val aktivitetId = arenaDataIdTranslationService.hentEllerOpprettAktivitetId(tiltakDeltaker.tiltakdeltakerId, Aktivitet.Type.TILTAKSAKTIVITET)

		val aktivitet = tiltakDeltaker.convertToAktivitet(
			aktivitetId = aktivitetId,
			personIdent = personIdent,
			aktivitetType = Aktivitet.Type.TILTAKSAKTIVITET,
			tiltakKode = gjennomforing.tiltakKode,
			arrangorNavn = gjennomforing.arrangorNavn,
			beskrivelse = gjennomforing.navn, //TODO: Denne skal bare være lokaltnavn på jobbklubb
			tiltakNavn = gjennomforing.navn //TODO: Lage mapping for å sette denne
		)

		arenaDataIdTranslationService.upsertTranslation(
			arenaId = tiltakDeltaker.tiltakdeltakerId,
			aktivitetId = aktivitetId,
			aktivitetType = Aktivitet.Type.TILTAKSAKTIVITET
		)

		val amtData = KafkaMessageDto(
			type = PayloadType.AKTIVITET,
			operation = message.operationType,
			payload = aktivitet
		)

		kafkaProducerService.sendTilAmtTiltak(aktivitet.id, amtData)

		arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(tiltakDeltaker.tiltakdeltakerId))

		secureLog.info("Melding for deltaker id=$aktivitetId arenaId=${tiltakDeltaker.tiltakdeltakerId} personId=${tiltakDeltaker.personId} fnr=$personIdent er sendt")
		log.info("Melding for deltaker id=$aktivitetId arenaId=${tiltakDeltaker.tiltakdeltakerId} transactionId=${amtData.transactionId} op=${amtData.operation} er sendt")
		metrics.publishMetrics(message)
	}

	private fun harStatusSomSkalIgnoreres(arenaDeltakerStatusKode: String): Boolean {
		val statuserSomIgnoreres = listOf("VENTELISTE", "AKTUELL", "JATAKK", "INFOMOETE")
		return statuserSomIgnoreres.contains(arenaDeltakerStatusKode)
	}

}
