package no.nav.arena_tiltak_aktivitet_acl.services

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetskortHeaders
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.KafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.utils.ObjectMapper
import no.nav.common.kafka.producer.KafkaProducerClient
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.LoggerFactory
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

	private val log = LoggerFactory.getLogger(javaClass)

	fun sendTilAktivitetskortTopic(
		messageKey: UUID,
		data: KafkaMessageDto,
		aktivitetskortHeaders: AktivitetskortHeaders,
	) {
		val headers = listOf(
			RecordHeader("arenaTiltakskode", aktivitetskortHeaders.tiltakKode.toByteArray()),
			RecordHeader("eksternReferanseId", aktivitetskortHeaders.arenaId.toByteArray()),
			RecordHeader("oppfolgingsperiode", aktivitetskortHeaders.oppfolgingsperiode?.toString()?.toByteArray() ?: "".toByteArray()),
			RecordHeader("oppfolgingsperiodeSlutt", aktivitetskortHeaders.oppfolgingsperiode?.toString()?.toByteArray() ?: "".toByteArray()),
			RecordHeader("historisk", aktivitetskortHeaders.historisk?.toString()?.toByteArray() ?: "".toByteArray())
		)

		val record = ProducerRecord(topic, null, messageKey.toString(), objectMapper.writeValueAsString(data), headers)
		kafkaProducer.sendSync(record)
			.also { log.debug("Sendt message to AKAAS: offset=${it.offset()} partition=${it.partition()} topic=${it.topic()}") }
	}

	companion object {
		const val TILTAK_ID_PREFIX = "ARENATA"
	}

}
