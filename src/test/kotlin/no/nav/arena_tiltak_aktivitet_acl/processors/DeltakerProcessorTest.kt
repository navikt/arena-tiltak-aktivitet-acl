package no.nav.arena_tiltak_aktivitet_acl.processors

import ArenaOrdsProxyClient
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.util.*
import no.nav.arena_tiltak_aktivitet_acl.database.DatabaseTestUtils
import no.nav.arena_tiltak_aktivitet_acl.database.SingletonPostgresContainer
import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.exceptions.DependencyNotIngestedException
import no.nav.arena_tiltak_aktivitet_acl.exceptions.IgnoredException
import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.GjennomforingRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.TiltakRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.TranslationRepository
import no.nav.arena_tiltak_aktivitet_acl.services.AktivitetService
import no.nav.arena_tiltak_aktivitet_acl.services.KafkaProducerService
import no.nav.arena_tiltak_aktivitet_acl.services.TiltakService
import no.nav.arena_tiltak_aktivitet_acl.services.TranslationService
import no.nav.arena_tiltak_aktivitet_acl.utils.ARENA_DELTAKER_TABLE_NAME
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate


class DeltakerProcessorTest : FunSpec({

	val dataSource = SingletonPostgresContainer.getDataSource()

	val ordsClient = mock<ArenaOrdsProxyClient> {
		on { hentFnr(anyLong()) } doReturn "01010051234"
	}

	val kafkaProducerService = mock<KafkaProducerService>()

	lateinit var arenaDataRepository: ArenaDataRepository
	lateinit var idTranslationRepository: TranslationRepository

	lateinit var deltakerProcessor: DeltakerProcessor

	val nonIgnoredGjennomforingArenaId = 1L
	val ignoredGjennomforingArenaId = 2L

	beforeEach {
		// val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
		// rootLogger.level = Level.WARN

		val template = NamedParameterJdbcTemplate(dataSource)
		arenaDataRepository = ArenaDataRepository(template)
		idTranslationRepository = TranslationRepository(template)

		DatabaseTestUtils.cleanAndInitDatabase(dataSource, "/deltaker-processor_test-data.sql")

		deltakerProcessor = DeltakerProcessor(
			arenaDataRepository = arenaDataRepository,
			arenaIdTranslationService = TranslationService(idTranslationRepository),
			ordsClient = ordsClient,
			kafkaProducerService = kafkaProducerService,
			aktivitetService = AktivitetService(AktivitetRepository(template)),
			gjennomforingRepository = GjennomforingRepository(template),
			tiltakService = TiltakService(TiltakRepository(template))
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

	test("DeltakerProcessor should get a translation on non-ignored Gjennomforing") {
		val position = UUID.randomUUID().toString()

		val newDeltaker = createArenaDeltakerKafkaMessage(
			tiltakGjennomforingArenaId = nonIgnoredGjennomforingArenaId,
			deltakerArenaId = 1L
		)

		deltakerProcessor.handleArenaMessage(newDeltaker)

		getAndCheckArenaDataRepositoryEntry(operation = Operation.CREATED, position)

		val translationEntry = idTranslationRepository.get(1, AktivitetKategori.TILTAKSAKTIVITET)

		translationEntry shouldNotBe null
	}

	test("Skal kaste ignored exception for ignorerte statuser") {
		val statuser = listOf("VENTELISTE", "AKTUELL", "JATAKK", "INFOMOETE")

		shouldThrowExactly<IgnoredException> {
			deltakerProcessor.handleArenaMessage(createArenaDeltakerKafkaMessage(
				tiltakGjennomforingArenaId = ignoredGjennomforingArenaId,
				deltakerArenaId = 1,
				deltakerStatusKode = "AKTUELL"
			))
		}

		statuser.forEachIndexed { idx, status ->
			shouldThrowExactly<IgnoredException> {
				deltakerProcessor.handleArenaMessage(createArenaDeltakerKafkaMessage(
					tiltakGjennomforingArenaId = nonIgnoredGjennomforingArenaId,
					deltakerArenaId = idx.toLong() + 1,
					deltakerStatusKode = status
				))
			}
		}
	}

	test("Insert Deltaker with gjennomføring not processed should throw exception") {
		shouldThrowExactly<DependencyNotIngestedException> {
			deltakerProcessor.handleArenaMessage(
				createArenaDeltakerKafkaMessage(
					2348790L,
					1L
				)
			)
		}
	}

	test("Insert Deltaker on Ignored Gjennomføring sets Deltaker to Ingored") {
		shouldThrowExactly<IgnoredException> {
			deltakerProcessor.handleArenaMessage(
				createArenaDeltakerKafkaMessage(
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
				tiltakGjennomforingArenaId = nonIgnoredGjennomforingArenaId,
				deltakerArenaId = 1L,
				operation = Operation.DELETED
			)
		)

		getAndCheckArenaDataRepositoryEntry(Operation.DELETED, position, IngestStatus.HANDLED)
	}

})


