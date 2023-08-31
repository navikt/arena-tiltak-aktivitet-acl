package no.nav.arena_tiltak_aktivitet_acl.integration.executors

import io.kotest.assertions.fail
import io.kotest.common.runBlocking
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.db.TranslationDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker.AktivitetResult
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker.HandledResult
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.gruppetiltak.GruppeTiltakCommand
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.gruppetiltak.GruppeTiltakResult
import no.nav.arena_tiltak_aktivitet_acl.integration.kafka.KafkaAktivitetskortIntegrationConsumer
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.TranslationRepository
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import no.nav.common.kafka.producer.KafkaProducerClientImpl

class GruppeTiltakExecutor(
	kafkaProducer: KafkaProducerClientImpl<String, String>,
	arenaDataRepository: ArenaDataRepository,
	translationRepository: TranslationRepository
): TestExecutor(
	kafkaProducer = kafkaProducer,
	arenaDataRepository = arenaDataRepository,
	translationRepository = translationRepository
){

	companion object {
		private val messageFlow = MutableSharedFlow<TestRecord>(replay = 1)
		init {
			KafkaAktivitetskortIntegrationConsumer.subscribeAktivitet { record, headers ->
				messageFlow.emit(TestRecord(record,headers))
			}
		}
	}

	private suspend fun waitForRecord(isCorrectRecord: (TestRecord) -> Boolean): TestRecord {
		return withTimeout(5000) {
			messageFlow.first { isCorrectRecord(it) }
		}
	}

	private val topic = "gjennomforing"
	fun execute(command: GruppeTiltakCommand): AktivitetResult {
		val kafkaMessage = command.toArenaKafkaMessageDto(incrementAndGetPosition())
		sendKafkaMessage(
			topic,
			objectMapper.writeValueAsString(kafkaMessage),
			command.key)
		val arenaData = getArenaData(
			ArenaTableName.GRUPPETILTAK,
			Operation.fromArenaOperationString(kafkaMessage.opType),
			kafkaMessage.pos
		)
		var translation: TranslationDbo? = null
		// There is no ack for messages which are put in retry,
		// use translation-table for checking if record is processed
		when (arenaData.ingestStatus) {
			IngestStatus.IGNORED, IngestStatus.INVALID -> {}
			IngestStatus.NEW -> {}
			IngestStatus.QUEUED, IngestStatus.RETRY, IngestStatus.FAILED -> {
				translation = getTranslationRetry(arenaData.arenaId.toLong(), AktivitetKategori.GRUPPEAKTIVITET)
			}
			IngestStatus.HANDLED -> {
				translation = getTranslationRetry(arenaData.arenaId.toLong(), AktivitetKategori.GRUPPEAKTIVITET)
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
		return AktivitetResult(arenaData.operationPosition, arenaData, translation)
	}
}
