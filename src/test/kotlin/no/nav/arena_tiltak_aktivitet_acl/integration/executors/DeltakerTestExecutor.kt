package no.nav.arena_tiltak_aktivitet_acl.integration.executors

import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.db.TranslationDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetskortHeaders
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.KafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker.AktivitetResult
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker.DeltakerCommand
import no.nav.arena_tiltak_aktivitet_acl.integration.kafka.KafkaAktivitetskortIntegrationConsumer
import no.nav.arena_tiltak_aktivitet_acl.integration.utils.Retry.nullableAsyncRetryHandler
import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetDbo
import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.TranslationRepository
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import org.slf4j.LoggerFactory
import java.util.*

class DeltakerTestExecutor(
	kafkaProducer: KafkaProducerClientImpl<String, String>,
	arenaDataRepository: ArenaDataRepository,
	translationRepository: TranslationRepository,
	private val aktivitetRepository: AktivitetRepository
) : TestExecutor(
	kafkaProducer = kafkaProducer,
	arenaDataRepository = arenaDataRepository,
	translationRepository = translationRepository,
) {

	private val log = LoggerFactory.getLogger(javaClass)
	private val topic = "deltaker"
	private val outputMessages = mutableMapOf<UUID, MutableList<TestRecord>>()

	init {
		KafkaAktivitetskortIntegrationConsumer
			.subscribeAktivitet { kafkaMessageDto, aktivitetkortHeader ->
				val messagesOnKey = outputMessages[kafkaMessageDto.aktivitetskort.id] ?: mutableListOf()
				messagesOnKey.add(TestRecord(kafkaMessageDto ,aktivitetkortHeader))
				outputMessages[kafkaMessageDto.aktivitetskort.id] = messagesOnKey
			}
	}

	fun execute(command: DeltakerCommand): AktivitetResult {
		return sendAndCheck(
			command.toArenaKafkaMessageDto(incrementAndGetPosition()),
			command.tiltakDeltakerId.toString()
		)
	}

	private fun sendAndCheck(wrapper: ArenaKafkaMessageDto, tiltakDeltakerId: String): AktivitetResult {
		sendKafkaMessage(topic, objectMapper.writeValueAsString(wrapper), tiltakDeltakerId)
		return getResults(wrapper)
	}

	private fun getResults(wrapper: ArenaKafkaMessageDto): AktivitetResult {
		val arenaData = getArenaData(
			ArenaTableName.DELTAKER,
			Operation.fromArenaOperationString(wrapper.opType),
			wrapper.pos
		)

		var message: TestRecord? = null
		var translation: TranslationDbo? = null
		// There is no ack for messages which are put in retry,
		// use translation-table for checking if record is processed
		when (arenaData.ingestStatus) {
			IngestStatus.IGNORED -> {}
			IngestStatus.QUEUED, IngestStatus.RETRY, IngestStatus.FAILED -> {

				translation = getTranslationRetry(arenaData.arenaId.toLong(), AktivitetKategori.TILTAKSAKTIVITET)
			}
			IngestStatus.HANDLED -> {
				translation = getTranslationRetry(arenaData.arenaId.toLong(), AktivitetKategori.TILTAKSAKTIVITET)
				message = getOutputMessageRetry { translation!!.aktivitetId == it.melding.aktivitetskort.id }
			}
		}

		return AktivitetResult(
			arenaData.operationPosition,
			arenaData,
			translation,
			message?.melding,
			message?.headers,
		)
	}

	private fun getAktivitetRetry(aktivitetId: UUID): AktivitetDbo? {
		return nullableAsyncRetryHandler("get aktivitet $aktivitetId") { aktivitetRepository.getAktivitet(aktivitetId) }
	}

	private fun getOutputMessageBy(predicate: (TestRecord) -> Boolean): Pair<UUID, TestRecord>? {
		return outputMessages.entries
			.firstOrNull { (_, messages) -> messages.lastOrNull()?.let(predicate) ?: false }
			?.let { it.key to it.value.last() }
	}

	private fun getOutputMessageRetry(predicate: (TestRecord) -> Boolean): TestRecord? {
		var attempts = 0
		while (attempts < 20) {
			val result = getOutputMessageBy(predicate)
			if (result != null) {
				val (aktivitetId, message) = result
				log.info("Found deltaker message")
				outputMessages[aktivitetId]!!.remove(message)
				return message
			}

			log.info("Retrying for deltaker message ${outputMessages}")
			Thread.sleep(250)
			attempts++
		}
		return null
	}
}

data class TestRecord(
	val melding: KafkaMessageDto,
	val headers: AktivitetskortHeaders
)
