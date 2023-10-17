package no.nav.arena_tiltak_aktivitet_acl.integration.executors

import io.kotest.common.runBlocking
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeout
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetskortHeaders
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.KafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker.*
import no.nav.arena_tiltak_aktivitet_acl.integration.kafka.KafkaAktivitetskortIntegrationConsumer
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.DeltakerAktivitetMappingRepository
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import no.nav.common.kafka.producer.KafkaProducerClientImpl

class DeltakerTestExecutor(
	kafkaProducer: KafkaProducerClientImpl<String, String>,
	arenaDataRepository: ArenaDataRepository,
	val deltakerAktivitetMappingRepository: DeltakerAktivitetMappingRepository
) : TestExecutor(
	kafkaProducer = kafkaProducer,
	arenaDataRepository = arenaDataRepository,
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

	fun execute(command: DeltakerCommand, expectAktivitetskortOnTopic: Boolean = true): AktivitetResult {
		return sendAndCheck(
			command.toArenaKafkaMessageDto(incrementAndGetPosition()),
			command.tiltakDeltakerId.toString(),
			expectAktivitetskortOnTopic
		)
	}
	private suspend fun waitForAktivitetskortOnOutgoingTopic(isCorrectRecord: (TestRecord) -> Boolean): TestRecord {
		return withTimeout(5000) {
			messageFlow.first { isCorrectRecord(it) }
		}
	}

	private fun sendAndCheck(wrapper: ArenaKafkaMessageDto, tiltakDeltakerId: String, expectAktivitetskortOnTopic: Boolean): AktivitetResult {
		sendKafkaMessage(topic, objectMapper.writeValueAsString(wrapper), tiltakDeltakerId)
		return getResults(wrapper, expectAktivitetskortOnTopic)
	}

	private fun getResults(wrapper: ArenaKafkaMessageDto, expectAktivitetskortOnTopic: Boolean): AktivitetResult {
		val arenaData = pollArenaData(
			ArenaTableName.DELTAKER,
			Operation.fromArenaOperationString(wrapper.opType),
			wrapper.pos
		)

		val deltakelseId = DeltakelseId(arenaData.arenaId.toLong())
		var deltakerAktivitetMapping = deltakerAktivitetMappingRepository.get(deltakelseId, AktivitetKategori.TILTAKSAKTIVITET)
		// There is no ack for messages which are put in retry,
		// use translation-table for checking if record is processed <- GJELDER IKKE LENGER
		when (arenaData.ingestStatus) {
			IngestStatus.IGNORED, IngestStatus.INVALID -> {}
			IngestStatus.NEW -> {}
			IngestStatus.QUEUED, IngestStatus.RETRY, IngestStatus.FAILED -> {}
			IngestStatus.HANDLED -> {
				if (!expectAktivitetskortOnTopic) {
					return HandledAndIgnored(
						arenaData.operationPosition,
						arenaData,
						deltakerAktivitetMapping
					)
				}

				val message: TestRecord = runBlocking {
					waitForAktivitetskortOnOutgoingTopic {
						deltakerAktivitetMapping = deltakerAktivitetMappingRepository.get(deltakelseId, AktivitetKategori.TILTAKSAKTIVITET)
						deltakerAktivitetMapping.any { a -> it.melding.aktivitetskort.id == a.aktivitetId }
					}
				}
				return HandledResult(
					arenaData.operationPosition,
					arenaData,
					deltakerAktivitetMapping,
					message.melding,
					message.headers,
				)
			}
		}
		return AktivitetResult(
			arenaData.operationPosition,
			arenaData,
			deltakerAktivitetMapping
		)
	}
}

data class TestRecord(
	val melding: KafkaMessageDto,
	val headers: AktivitetskortHeaders
)
