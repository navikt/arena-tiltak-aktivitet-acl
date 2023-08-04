package no.nav.arena_tiltak_aktivitet_acl.integration

import no.nav.arena_tiltak_aktivitet_acl.auth.Issuer
import no.nav.arena_tiltak_aktivitet_acl.auth.M2M_ROLE
import no.nav.arena_tiltak_aktivitet_acl.database.DatabaseTestUtils
import no.nav.arena_tiltak_aktivitet_acl.database.SingletonPostgresContainer
import no.nav.arena_tiltak_aktivitet_acl.integration.executors.DeltakerTestExecutor
import no.nav.arena_tiltak_aktivitet_acl.integration.executors.GjennomforingTestExecutor
import no.nav.arena_tiltak_aktivitet_acl.integration.executors.TiltakTestExecutor
import no.nav.arena_tiltak_aktivitet_acl.integration.kafka.KafkaAktivitetskortIntegrationConsumer
import no.nav.arena_tiltak_aktivitet_acl.kafka.KafkaProperties
import no.nav.arena_tiltak_aktivitet_acl.kafka.KafkaTopicProperties
import no.nav.arena_tiltak_aktivitet_acl.mocks.OrdsClientMock
import no.nav.arena_tiltak_aktivitet_acl.repositories.*
import no.nav.arena_tiltak_aktivitet_acl.services.RetryArenaMessageProcessorService
import no.nav.arena_tiltak_aktivitet_acl.services.TiltakService
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.test.context.ActiveProfiles
import java.util.*
import javax.sql.DataSource


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(IntegrationTestConfiguration::class)
@ActiveProfiles("integration")
@EnableMockOAuth2Server
abstract class IntegrationTestBase {

	@Autowired
	lateinit var dataSource: DataSource

	@Autowired
	private lateinit var retryArenaMessageProcessorService: RetryArenaMessageProcessorService

	@Autowired
	lateinit var tiltakService: TiltakService

	@Autowired
	lateinit var tiltakExecutor: TiltakTestExecutor

	@Autowired
	lateinit var gjennomforingExecutor: GjennomforingTestExecutor

	@Autowired
	lateinit var deltakerExecutor: DeltakerTestExecutor

	@Autowired
	lateinit var mockOAuthServer: MockOAuth2Server

	@LocalServerPort
	var port: Int? = null


	@BeforeEach
	fun beforeEach() {
		DatabaseTestUtils.cleanDatabase(dataSource)
	}

	@AfterEach
	fun cleanup() {
		tiltakService.invalidateTiltakByKodeCache()
		OrdsClientMock.fnrHandlers.clear()
		OrdsClientMock.virksomhetsHandler.clear()
	}

	fun processMessages() {
		retryArenaMessageProcessorService.processMessages()
	}

	fun processFailedMessages() {
		retryArenaMessageProcessorService.processFailedMessages()
	}

	fun issueAzureAdM2MToken(
		subject: String = UUID.randomUUID().toString(),
	): String {
		val claimsWithRoles = mapOf(
			"roles" to arrayOf(M2M_ROLE),
			"oid" to subject)
		return mockOAuthServer.issueToken(Issuer.AZURE_AD, subject, "aktivitet-arena-acl", claimsWithRoles).serialize()
	}
}

@Profile("integration")
@TestConfiguration
open class IntegrationTestConfiguration(
	val kafkaTopicProperties: KafkaTopicProperties
) {

	@Value("\${app.env.aktivitetskortTopic}")
	lateinit var consumerTopic: String

	@Bean
	open fun tiltakExecutor(
		kafkaProducer: KafkaProducerClientImpl<String, String>,
		arenaDataRepository: ArenaDataRepository,
		translationRepository: TranslationRepository,
		tiltakRepository: TiltakRepository
	): TiltakTestExecutor {
		return TiltakTestExecutor(kafkaProducer, arenaDataRepository, translationRepository, tiltakRepository)
	}

	@Bean
	open fun gjennomforingExecutor(
		kafkaProducer: KafkaProducerClientImpl<String, String>,
		arenaDataRepository: ArenaDataRepository,
		gjennomforingRepository: GjennomforingRepository,
		translationRepository: TranslationRepository
	): GjennomforingTestExecutor {
		return GjennomforingTestExecutor(kafkaProducer, arenaDataRepository, gjennomforingRepository, translationRepository)
	}

	@Bean
	open fun deltakerExecutor(
		kafkaProducer: KafkaProducerClientImpl<String, String>,
		arenaDataRepository: ArenaDataRepository,
		translationRepository: TranslationRepository,
		aktivitetRepository: AktivitetRepository
	): DeltakerTestExecutor {
		return DeltakerTestExecutor(kafkaProducer, arenaDataRepository, translationRepository)
	}

	@Bean
	open fun dataSource(): DataSource {
		return SingletonPostgresContainer.getDataSource()
	}

	@Bean
	open fun kafkaProperties(testConsumer: Properties, testProducer: Properties): KafkaProperties {
		return object : KafkaProperties {
			override fun consumer(): Properties {
				return testConsumer
			}
			override fun producer(): Properties {
				return testProducer
			}
		}
	}

	@Bean
	open fun kafkaAmtIntegrationConsumer(properties: KafkaProperties): KafkaAktivitetskortIntegrationConsumer {
		return KafkaAktivitetskortIntegrationConsumer(properties, consumerTopic)
	}

	@Value("\${app.env.aktivitetskortTopic}")
	lateinit var aktivitetskortTopic: String

	@Bean
	open fun embeddedKafka(): EmbeddedKafkaBroker {
		return EmbeddedKafkaBroker(
			1,
			false,
			1,
			kafkaTopicProperties.arenaTiltakTopic,
			kafkaTopicProperties.arenaTiltakDeltakerTopic,
			kafkaTopicProperties.arenaTiltakGjennomforingTopic,
			aktivitetskortTopic
		)
	}

	@Bean
	open fun testConsumer(embeddedKafkaBroker: EmbeddedKafkaBroker): Properties {
		return KafkaPropertiesBuilder.consumerBuilder()
			.withBaseProperties()
			.withConsumerGroupId("consumer")
			.withBrokerUrl(embeddedKafkaBroker.getBrokersAsString())
			.withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
			.withPollProperties(10, 30_000)
			.build();
	}

	@Bean
	open fun testProducer(embeddedKafkaBroker: EmbeddedKafkaBroker): Properties {
		return KafkaPropertiesBuilder.producerBuilder()
			.withBaseProperties()
			.withProducerId("producer")
			.withBrokerUrl(embeddedKafkaBroker.brokersAsString)
			.withSerializers(
				StringSerializer::class.java,
				StringSerializer::class.java
			)
			.build()
	}
}
