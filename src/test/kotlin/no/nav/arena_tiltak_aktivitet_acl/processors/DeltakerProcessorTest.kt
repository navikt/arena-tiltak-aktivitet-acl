package no.nav.arena_tiltak_aktivitet_acl.processors

import ArenaOrdsProxyClient
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging.OppfolgingClient
import no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging.Oppfolgingsperiode
import no.nav.arena_tiltak_aktivitet_acl.database.DatabaseTestUtils
import no.nav.arena_tiltak_aktivitet_acl.database.SingletonPostgresContainer
import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.exceptions.DependencyNotIngestedException
import no.nav.arena_tiltak_aktivitet_acl.exceptions.IgnoredException
import no.nav.arena_tiltak_aktivitet_acl.repositories.*
import no.nav.arena_tiltak_aktivitet_acl.services.*
import no.nav.arena_tiltak_aktivitet_acl.utils.ARENA_DELTAKER_TABLE_NAME
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.ZonedDateTime
import java.util.*

class DeltakerProcessorTest : FunSpec({

	val dataSource = SingletonPostgresContainer.getDataSource()

	val ordsClient = mock<ArenaOrdsProxyClient> {
		on { hentFnr(anyLong()) } doReturn "01010051234"
	}


	val oppfolgingClient = mock<OppfolgingClient> {
		on { hentOppfolgingsperioder(anyString()) } doReturn listOf(
			Oppfolgingsperiode(
				uuid = UUID.randomUUID(),
				startDato = ZonedDateTime.now().minusMonths(2),
				sluttDato = ZonedDateTime.now().minusMonths(1)
			),
			Oppfolgingsperiode(
				uuid = UUID.randomUUID(),
				startDato = ZonedDateTime.now().minusWeeks(2),
				sluttDato = null
			)
		)
	}

	val kafkaProducerService = mock<KafkaProducerService>()

	lateinit var arenaDataRepository: ArenaDataRepository
	lateinit var idTranslationRepository: TranslationRepository
	lateinit var personSporingRepository: PersonSporingRepository

	lateinit var deltakerProcessor: DeltakerProcessor

	val nonIgnoredGjennomforingArenaId = 1L
	val ignoredGjennomforingArenaId = 2L

	beforeEach {
		// val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
		// rootLogger.level = Level.WARN

		val template = NamedParameterJdbcTemplate(dataSource)
		arenaDataRepository = ArenaDataRepository(template)
		idTranslationRepository = TranslationRepository(template)
		personSporingRepository = PersonSporingRepository(template)

		DatabaseTestUtils.cleanAndInitDatabase(dataSource, "/deltaker-processor_test-data.sql")

		deltakerProcessor = DeltakerProcessor(
			arenaDataRepository = arenaDataRepository,
			arenaIdTranslationService = TranslationService(idTranslationRepository),
			ordsClient = ordsClient,
			kafkaProducerService = kafkaProducerService,
			aktivitetService = AktivitetService(AktivitetRepository(template)),
			gjennomforingRepository = GjennomforingRepository(template),
			tiltakService = TiltakService(TiltakRepository(template)),
			oppfolgingsperiodeService = OppfolgingsperiodeService(oppfolgingClient),
			personsporingService = PersonsporingService(personSporingRepository)
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
		val newDeltaker = createArenaDeltakerKafkaMessage(
			tiltakGjennomforingArenaId = nonIgnoredGjennomforingArenaId,
			deltakerArenaId = 1L
		)

		deltakerProcessor.handleArenaMessage(newDeltaker)


		getAndCheckArenaDataRepositoryEntry(operation = Operation.CREATED, (operationPos).toString())

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

			deltakerProcessor.handleArenaMessage(createArenaDeltakerKafkaMessage(
				tiltakGjennomforingArenaId = nonIgnoredGjennomforingArenaId,
				deltakerArenaId = idx.toLong() + 1,
				deltakerStatusKode = status
			))

			getAndCheckArenaDataRepositoryEntry(operation = Operation.CREATED, (operationPos).toString())
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

