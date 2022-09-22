package no.nav.arena_tiltak_aktivitet_acl.kafka

import no.nav.arena_tiltak_aktivitet_acl.services.ArenaMessageProcessorService
import no.nav.common.featuretoggle.UnleashClient
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
open class KafkaConsumer(
	kafkaTopicProperties: KafkaTopicProperties,
	kafkaProperties: KafkaProperties,
	private val arenaMessageProcessorService: ArenaMessageProcessorService,
	unleashClient: UnleashClient,
) {

	private val client: KafkaConsumerClient

	private val log = LoggerFactory.getLogger(javaClass)

	init {
		val topics = listOf(
			kafkaTopicProperties.arenaTiltakTopic,
			kafkaTopicProperties.arenaTiltakGjennomforingTopic,
			kafkaTopicProperties.arenaTiltakDeltakerTopic,
		)

		val topicConfigs = topics.map { topic ->
			KafkaConsumerClientBuilder.TopicConfig<String, String>()
				.withLogging()
				.withConsumerConfig(
					topic,
					stringDeserializer(),
					stringDeserializer(),
					arenaMessageProcessorService::handleArenaGoldenGateRecord
				)
		}

		client = KafkaConsumerClientBuilder.builder()
			.withProperties(kafkaProperties.consumer())
			.withToggle { unleashClient.isEnabled("aktivitet-arena-acl.kafka.consumer.disabled") }
			.withTopicConfigs(topicConfigs)
			.build()
	}

	@EventListener
	open fun onApplicationEvent(_event: ContextRefreshedEvent?) {
		log.info("Starting kafka consumer...")
		client.start()
	}

}
