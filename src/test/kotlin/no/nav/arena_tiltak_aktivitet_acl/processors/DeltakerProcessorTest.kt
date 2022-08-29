package no.nav.arena_tiltak_aktivitet_acl.processors

import ArenaOrdsProxyClient
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.arena_tiltak_aktivitet_acl.database.DatabaseTestUtils
import no.nav.arena_tiltak_aktivitet_acl.database.SingletonPostgresContainer
import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.exceptions.DependencyNotIngestedException
import no.nav.arena_tiltak_aktivitet_acl.exceptions.IgnoredException
import no.nav.arena_tiltak_aktivitet_acl.metrics.DeltakerMetricHandler
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataTranslationRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.services.ArenaDataIdTranslationService
import no.nav.arena_tiltak_aktivitet_acl.services.KafkaProducerService
import no.nav.arena_tiltak_aktivitet_acl.utils.ARENA_DELTAKER_TABLE_NAME
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.*
/*
class DeltakerProcessorTest : FunSpec({

	val dataSource = SingletonPostgresContainer.getDataSource()

	val ordsClient = mock<ArenaOrdsProxyClient> {
		on { hentFnr(anyLong()) } doReturn "01010051234"
	}

	val kafkaProducerService = mock<KafkaProducerService>()

	lateinit var arenaDataRepository: ArenaDataRepository
	lateinit var idTranslationRepository: ArenaDataTranslationRepository

	lateinit var deltakerProcessor: DeltakerProcessor

	val nonIgnoredGjennomforingArenaId = 1L
	val ignoredGjennomforingArenaId = 2L

	beforeEach {
		val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
		rootLogger.level = Level.WARN

		val template = NamedParameterJdbcTemplate(dataSource)
		arenaDataRepository = ArenaDataRepository(template)
		idTranslationRepository = ArenaDataTranslationRepository(template)

		DatabaseTestUtils.cleanAndInitDatabase(dataSource, "/deltaker-processor_test-data.sql")

		deltakerProcessor = DeltakerProcessor(
			arenaDataRepository = arenaDataRepository,
			arenaDataIdTranslationService = ArenaDataIdTranslationService(idTranslationRepository),
			ordsClient = ordsClient,
			meterRegistry = SimpleMeterRegistry(),
			kafkaProducerService = kafkaProducerService,
			metrics = DeltakerMetricHandler(SimpleMeterRegistry())
		)
	}

	fun getAndCheckArenaDataRepositoryEntry(
		operation: Operation,
		position: String,
		expectedStatus: IngestStatus = IngestStatus.HANDLED
	): ArenaDataDbo {
		val arenaDataRepositoryEntry = shouldNotThrowAny {
			arenaDataRepository.get(ARENA_DELTAKER_TABLE_NAME, operation, position)
		}

		arenaDataRepositoryEntry shouldNotBe null
		arenaDataRepositoryEntry.ingestStatus shouldBe expectedStatus

		when (arenaDataRepositoryEntry.operation) {
			Operation.CREATED -> {
				arenaDataRepositoryEntry.before shouldBe null
				arenaDataRepositoryEntry.after shouldNotBe null
			}
			Operation.MODIFIED -> {
				arenaDataRepositoryEntry.before shouldNotBe null
				arenaDataRepositoryEntry.after shouldNotBe null
			}
			Operation.DELETED -> {
				arenaDataRepositoryEntry.before shouldNotBe null
				arenaDataRepositoryEntry.after shouldBe null
			}
		}

		return arenaDataRepositoryEntry
	}


	test("Insert Deltaker on non-ignored Gjennomforing") {
		val position = UUID.randomUUID().toString()

		val newDeltaker = createArenaDeltakerKafkaMessage(
			position = position,
			tiltakGjennomforingArenaId = nonIgnoredGjennomforingArenaId,
			deltakerArenaId = 1L
		)

		deltakerProcessor.handleArenaMessage(newDeltaker)

		getAndCheckArenaDataRepositoryEntry(operation = Operation.CREATED, position)

		val translationEntry = idTranslationRepository.get("1")

		translationEntry shouldNotBe null
	}

	test("Skal kaste ignored exception for ignorerte statuser") {
		val statuser = listOf("VENTELISTE", "AKTUELL", "JATAKK", "INFOMOETE")

		statuser.forEachIndexed { idx, status ->
			shouldThrowExactly<IgnoredException> {
				deltakerProcessor.handleArenaMessage(createArenaDeltakerKafkaMessage(
					position = UUID.randomUUID().toString(),
					tiltakGjennomforingArenaId = nonIgnoredGjennomforingArenaId,
					deltakerArenaId = idx.toLong(),
					deltakerStatusKode = status
				))
			}
		}
	}

	test("Insert Deltaker with gjennomføring not processed should throw exception") {
		val position = UUID.randomUUID().toString()

		shouldThrowExactly<DependencyNotIngestedException> {
			deltakerProcessor.handleArenaMessage(
				createArenaDeltakerKafkaMessage(
					position,
					2348790L,
					1L
				)
			)
		}
	}

	test("Insert Deltaker on Ignored Gjennomføring sets Deltaker to Ingored") {
		val position = UUID.randomUUID().toString()

		shouldThrowExactly<IgnoredException> {
			deltakerProcessor.handleArenaMessage(
				createArenaDeltakerKafkaMessage(
					position,
					ignoredGjennomforingArenaId,
					1L
				)
			)
		}
	}

	test("Should process deleted deltaker") {
		val position = UUID.randomUUID().toString()

		deltakerProcessor.handleArenaMessage(
			createArenaDeltakerKafkaMessage(
				position = position,
				tiltakGjennomforingArenaId = nonIgnoredGjennomforingArenaId,
				deltakerArenaId = 1L,
				operation = Operation.DELETED
			)
		)

		getAndCheckArenaDataRepositoryEntry(Operation.DELETED, position, IngestStatus.HANDLED)
	}

})
*/

