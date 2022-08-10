package no.nav.arena_tiltak_aktivitet_acl.integration.executors

import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataIdTranslationDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.amt.AmtOperation
import no.nav.arena_tiltak_aktivitet_acl.integration.utils.asyncRetryHandler
import no.nav.arena_tiltak_aktivitet_acl.integration.utils.nullableAsyncRetryHandler
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataIdTranslationRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.utils.ObjectMapperFactory
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import org.apache.kafka.clients.producer.ProducerRecord

abstract class TestExecutor(
	private val kafkaProducer: KafkaProducerClientImpl<String, String>,
	private val arenaDataRepository: ArenaDataRepository,
	private val translationRepository: ArenaDataIdTranslationRepository
) {

	companion object {
		var position = 0
	}

	val objectMapper = ObjectMapperFactory.get()

	fun incrementAndGetPosition(): String {
		return "${position++}"
	}

	fun sendKafkaMessage(topic: String, payload: String) {
		kafkaProducer.send(ProducerRecord(topic, payload))
	}

	fun getArenaData(table: String, operation: AmtOperation, position: String): ArenaDataDbo {
		return asyncRetryHandler({
			arenaDataRepository.getAll().find {
				it.arenaTableName == table
					&& it.operation == operation
					&& it.operationPosition == position
			}
		})
	}

	fun getTranslation(table: String, arenaId: String): ArenaDataIdTranslationDbo? {
		return nullableAsyncRetryHandler({ translationRepository.get(table, arenaId) })
	}
}
