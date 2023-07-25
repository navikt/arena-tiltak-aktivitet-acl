package no.nav.arena_tiltak_aktivitet_acl.integration.executors

import io.kotest.assertions.fail
import io.kotest.common.runBlocking
import kotlinx.coroutines.flow.*
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.db.TranslationDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetskortHeaders
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.KafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker.AktivitetResult
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker.DeltakerCommand
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker.HandledResult
import no.nav.arena_tiltak_aktivitet_acl.integration.kafka.KafkaAktivitetskortIntegrationConsumer
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.TranslationRepository
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import no.nav.common.kafka.producer.KafkaProducerClientImpl

class DeltakerTestExecutor(
	kafkaProducer: KafkaProducerClientImpl<String, String>,
	arenaDataRepository: ArenaDataRepository,
	translationRepository: TranslationRepository,
) : TestExecutor(
	kafkaProducer = kafkaProducer,
	arenaDataRepository = arenaDataRepository,
	translationRepository = translationRepository,
) {

	private val topic = "deltaker"

	companion object {
		private val messageFlow = MutableSharedFlow<TestRecord>(replay = 1)
		init {
			KafkaAktivitetskortIntegrationConsumer.subscribeAktivitet { record, headers ->
				messageFlow.emit(TestRecord(record,headers))
			}
		}
	}

	fun execute(command: DeltakerCommand): AktivitetResult {
		return sendAndCheck(
			command.toArenaKafkaMessageDto(incrementAndGetPosition()),
			command.tiltakDeltakerId.toString()
		)
	}
	private suspend fun waitForRecord(isCorrectRecord: (TestRecord) -> Boolean): TestRecord {
		return messageFlow.first { isCorrectRecord(it) }
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

		var translation: TranslationDbo? = null
		// There is no ack for messages which are put in retry,
		// use translation-table for checking if record is processed
		when (arenaData.ingestStatus) {
			IngestStatus.IGNORED, IngestStatus.INVALID -> {}
			IngestStatus.NEW -> {}
			IngestStatus.QUEUED, IngestStatus.RETRY, IngestStatus.FAILED -> {
				translation = getTranslationRetry(arenaData.arenaId.toLong(), AktivitetKategori.TILTAKSAKTIVITET)
			}
			IngestStatus.HANDLED -> {
				translation = getTranslationRetry(arenaData.arenaId.toLong(), AktivitetKategori.TILTAKSAKTIVITET)
					?: fail("Did not find translation after 20 attempts")
				val message: TestRecord = runBlocking {
					waitForRecord { translation.aktivitetId == it.melding.aktivitetskort.id }
				}
				return HandledResult(
					arenaData.operationPosition,
					arenaData,
					translation,
					message.melding,
					message.headers,
				)
			}
		}
		return AktivitetResult(
			arenaData.operationPosition,
			arenaData,
			translation
		)
	}
}

data class TestRecord(
	val melding: KafkaMessageDto,
	val headers: AktivitetskortHeaders
)
