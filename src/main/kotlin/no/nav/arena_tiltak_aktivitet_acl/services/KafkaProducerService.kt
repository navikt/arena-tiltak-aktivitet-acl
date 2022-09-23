package no.nav.arena_tiltak_aktivitet_acl.services

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.KafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.utils.ObjectMapper
import no.nav.common.kafka.producer.KafkaProducerClient
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*

@Service
open class KafkaProducerService(
	private val kafkaProducer: KafkaProducerClient<String, String>
) {

	private val objectMapper = ObjectMapper.get()

	@Value("\${app.env.aktivitetskortTopic}")
	lateinit var topic: String

	fun sendTilAktivitetskortTopic(messageKey: UUID, data: KafkaMessageDto<*>) {
		val record = ProducerRecord(topic, messageKey.toString(), objectMapper.writeValueAsString(data))
		kafkaProducer.sendSync(record)
	}

}
