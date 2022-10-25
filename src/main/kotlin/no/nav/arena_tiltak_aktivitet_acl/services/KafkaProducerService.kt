package no.nav.arena_tiltak_aktivitet_acl.services

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.KafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.utils.ObjectMapper
import no.nav.common.kafka.producer.KafkaProducerClient
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
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

	fun sendTilAktivitetskortTopic(messageKey: UUID, data: KafkaMessageDto, arenaTiltakskode: String, eksternReferanseId: String) {
		val headers = listOf(
			RecordHeader("arenaTiltakskode", arenaTiltakskode.toByteArray()),
			RecordHeader("eksternReferanseId", eksternReferanseId.toByteArray()),
		)

		val record = ProducerRecord(topic, null, messageKey.toString(), objectMapper.writeValueAsString(data), headers)
		kafkaProducer.sendSync(record)
	}

}
