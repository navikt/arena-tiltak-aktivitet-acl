package no.nav.arena_tiltak_aktivitet_acl.processors

/*
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


*/
