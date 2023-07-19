package no.nav.arena_tiltak_aktivitet_acl.integration.executors

import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.db.TranslationDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.integration.utils.asyncRetryHandler
import no.nav.arena_tiltak_aktivitet_acl.integration.utils.nullableAsyncRetryHandler
import no.nav.arena_tiltak_aktivitet_acl.repositories.TranslationRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import no.nav.arena_tiltak_aktivitet_acl.utils.ObjectMapper
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import org.apache.kafka.clients.producer.ProducerRecord

abstract class TestExecutor(
	private val kafkaProducer: KafkaProducerClientImpl<String, String>,
	private val arenaDataRepository: ArenaDataRepository,
	private val translationRepository: TranslationRepository,
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

	fun getArenaData(table: ArenaTableName, operation: Operation, position: String): ArenaDataDbo {
		return asyncRetryHandler({
			arenaDataRepository.getAll().find {
				it.arenaTableName == table
					&& it.operation == operation
					&& it.operationPosition == position
			}
		})
	}

	fun getTranslation(arenaId: Long, aktivitetKategori: AktivitetKategori): TranslationDbo? {
		return nullableAsyncRetryHandler({ translationRepository.get(arenaId, aktivitetKategori) })
	}


}
