package no.nav.arena_tiltak_aktivitet_acl.integration.executors

import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.OperationPos
import no.nav.arena_tiltak_aktivitet_acl.integration.utils.asyncRetryHandler
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import no.nav.arena_tiltak_aktivitet_acl.utils.ObjectMapper
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import org.apache.kafka.clients.producer.ProducerRecord

abstract class TestExecutor(
	private val kafkaProducer: KafkaProducerClientImpl<String, String>,
	private val arenaDataRepository: ArenaDataRepository,
) {

	companion object {
		var position = 0
	}

	val objectMapper = ObjectMapper.get()

	fun incrementAndGetPosition(): String {
		return "${position++}"
	}

	fun sendKafkaMessage(topic: String, payload: String, key: String) {
		kafkaProducer.send(ProducerRecord(topic, key, payload))
	}

	fun pollArenaData(table: ArenaTableName, operation: Operation, position: OperationPos): ArenaDataDbo {
		return asyncRetryHandler("get arenadata $table, $operation, $position") {
			arenaDataRepository.getAll().find {
				it.arenaTableName == table
					&& it.operation == operation
					&& it.operationPosition == position
			}
		}
	}

}
