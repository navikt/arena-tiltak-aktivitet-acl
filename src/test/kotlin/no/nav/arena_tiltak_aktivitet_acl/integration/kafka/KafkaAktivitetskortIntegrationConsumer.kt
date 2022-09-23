package no.nav.arena_tiltak_aktivitet_acl.integration.kafka

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.*
import no.nav.arena_tiltak_aktivitet_acl.kafka.KafkaProperties
import no.nav.arena_tiltak_aktivitet_acl.utils.JsonUtils
import no.nav.arena_tiltak_aktivitet_acl.utils.ObjectMapper
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.time.LocalDateTime
import java.util.*

class KafkaAktivitetskortIntegrationConsumer(
	kafkaProperties: KafkaProperties,
	topic: String
) {

	private val client: KafkaConsumerClient


	companion object {
		private val aktivitetSubscriptions = mutableMapOf<UUID, (wrapper: KafkaMessageDto<TiltakAktivitet>) -> Unit>()

		fun subscribeAktivitet(handler: (record: KafkaMessageDto<TiltakAktivitet>) -> Unit): UUID {
			val id = UUID.randomUUID()
			aktivitetSubscriptions[id] = handler

			return id
		}

		fun reset() {
			aktivitetSubscriptions.clear()
		}
	}


	init {
		val config = KafkaConsumerClientBuilder.TopicConfig<String, String>()
			.withLogging()
			.withConsumerConfig(
				topic,
				stringDeserializer(),
				stringDeserializer(),
				::handle
			)

		client = KafkaConsumerClientBuilder.builder()
			.withProperties(kafkaProperties.consumer())
			.withTopicConfig(config)
			.build()

		client.start()
	}

	private fun handle(record: ConsumerRecord<String, String>) {
		val unknownMessageWrapper = JsonUtils.fromJson(record.value(), UnknownMessageWrapper::class.java)

		when (unknownMessageWrapper.actionType) {
			ActionType.UPSERT_TILTAK_AKTIVITET_V1 -> {
				val deltakerPayload =
					ObjectMapper.get().treeToValue(unknownMessageWrapper.payload, TiltakAktivitet::class.java)
				val message = toKnownMessageWrapper(deltakerPayload, unknownMessageWrapper)
				aktivitetSubscriptions.values.forEach { it.invoke(message) }

			}
			else -> throw IllegalStateException("${unknownMessageWrapper.actionType} does not have a handler.")
		}
	}

	private fun <T> toKnownMessageWrapper(payload: T, unknownMessageWrapper: UnknownMessageWrapper): KafkaMessageDto<T> {
		return KafkaMessageDto(
			messageId = unknownMessageWrapper.messageId,
			source = unknownMessageWrapper.utsender,
			sendt = unknownMessageWrapper.sendt,
			actionType = unknownMessageWrapper.actionType,
			payload = payload
		)
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	data class UnknownMessageWrapper(
		val messageId: UUID,
		val utsender: String = "ARENA_TILTAK_AKTIVITET_ACL",
		val sendt: LocalDateTime,
		val actionType: ActionType,
		val payload: JsonNode
	)
}
