package no.nav.arena_tiltak_aktivitet_acl.services

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.KafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.utils.ObjectMapperFactory
import no.nav.common.kafka.producer.KafkaProducerClient
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*

@Service
open class KafkaProducerService(
	private val kafkaProducer: KafkaProducerClient<String, String>
) {

	private val objectMapper = ObjectMapperFactory.get()

	@Value("\${app.env.amtTopic}")
	lateinit var topic: String

	fun sendTilAmtTiltak(messageKey: UUID, data: KafkaMessageDto<*>) {
		val record = ProducerRecord(topic, messageKey.toString(), objectMapper.writeValueAsString(data))
		kafkaProducer.sendSync(record)
	}

}
