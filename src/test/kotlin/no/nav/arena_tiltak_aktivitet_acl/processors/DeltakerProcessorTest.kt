package no.nav.arena_tiltak_aktivitet_acl.processors

import ArenaOrdsProxyClient
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging.OppfolgingClient
import no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging.Oppfolgingsperiode
import no.nav.arena_tiltak_aktivitet_acl.database.DatabaseTestUtils
import no.nav.arena_tiltak_aktivitet_acl.database.SingletonPostgresContainer
import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.*
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import no.nav.arena_tiltak_aktivitet_acl.exceptions.DependencyNotIngestedException
import no.nav.arena_tiltak_aktivitet_acl.exceptions.IgnoredException
import no.nav.arena_tiltak_aktivitet_acl.exceptions.OppfolgingsperiodeNotFoundException
import no.nav.arena_tiltak_aktivitet_acl.mocks.OppfolgingClientMock
import no.nav.arena_tiltak_aktivitet_acl.repositories.*
import no.nav.arena_tiltak_aktivitet_acl.services.*
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

class DeltakerProcessorTest : FunSpec({
	val dataSource = SingletonPostgresContainer.getDataSource()

	val ordsClient by lazy {
		val client = mockk<ArenaOrdsProxyClient>()
		every { client.hentFnr(any<Long>()) } returns "01010051234"
		client
	}

	val defaultOppfolgingsperioder = listOf(
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

	val kafkaProducerService = mockk<KafkaProducerService>(relaxUnitFun = true)

	lateinit var arenaDataRepository: ArenaDataRepository
	lateinit var personSporingRepository: PersonSporingRepository
	lateinit var aktivitetRepository: AktivitetRepository
	lateinit var aktivitetskortIdRespository: AktivitetskortIdRepository
	lateinit var deltakelseLockRepository: DeltakelseLockRepository

	val logger = LoggerFactory.getLogger(javaClass)

	val aktivitetDboSlot = slot<AktivitetDbo>()
	val slowAktivitetRepository by lazy {
		val spyRepo = mockk<AktivitetRepository>()
		var delayed = true
		coEvery {spyRepo.upsert(aktivitet = capture(aktivitetDboSlot))} coAnswers {
			logger.info("In mockrepo for deltakelse: ${aktivitetDboSlot.captured.arenaId}")
			if (delayed) {
				delayed = false
				logger.info("Sleeping 50ms for deltakelse ${aktivitetDboSlot.captured.arenaId}")
				delay(50)
			}
			aktivitetRepository.upsert(aktivitetDboSlot.captured)
		}
		spyRepo
	}

	// Se SQL inserted før hver test
	val nonIgnoredGjennomforingArenaId = 1L
	val ignoredGjennomforingArenaId = 2L

	beforeEach {
		val template = NamedParameterJdbcTemplate(dataSource)
		arenaDataRepository = ArenaDataRepository(template)
		personSporingRepository = PersonSporingRepository(template)
		aktivitetRepository = AktivitetRepository(template)
		aktivitetskortIdRespository = AktivitetskortIdRepository(template)
		deltakelseLockRepository = DeltakelseLockRepository(template)
		clearMocks(kafkaProducerService)

		DatabaseTestUtils.cleanAndInitDatabase(dataSource, "/deltaker-processor_test-data.sql")
	}

	fun createDeltakerProcessor(oppfolgingsperioder: List<Oppfolgingsperiode> = defaultOppfolgingsperioder): DeltakerProcessor {
		val template = NamedParameterJdbcTemplate(dataSource)

		val oppfolgingClient = mockk<OppfolgingClient>()
		every { oppfolgingClient.hentOppfolgingsperioder(any()) } returns oppfolgingsperioder

		return DeltakerProcessor(
			arenaDataRepository = arenaDataRepository,
			kafkaProducerService = kafkaProducerService,
			aktivitetService = AktivitetService(AktivitetRepository(template), AktivitetskortIdRepository(template), DeltakelseLockRepository(template)),
			gjennomforingRepository = GjennomforingRepository(template),
			tiltakService = TiltakService(TiltakRepository(template)),
			oppfolgingsperiodeService = OppfolgingsperiodeService(oppfolgingClient),
			personsporingService = PersonsporingService(personSporingRepository, ordsClient),
			aktivitetskortIdService = AktivitetskortIdService(aktivitetRepository, aktivitetskortIdRespository, deltakelseLockRepository)
		)
	}

	fun getAndCheckArenaDataRepositoryEntry(
		operation: Operation,
		position: String,
		expectedStatus: IngestStatus = IngestStatus.HANDLED
	): ArenaDataDbo {
		val arenaDataRepositoryEntry = shouldNotThrowAny {
			arenaDataRepository.get(ArenaTableName.DELTAKER, operation, position)
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
		createDeltakerProcessor().handleArenaMessage(newDeltaker)
		getAndCheckArenaDataRepositoryEntry(operation = Operation.CREATED, (operationPos).toString())
		val translationEntry = aktivitetRepository.getCurrentAktivitetsId(DeltakelseId(1), AktivitetKategori.TILTAKSAKTIVITET)
		translationEntry shouldNotBe null
	}

	test("Skal ikke sende ut aktivitetskort for ignorerte statuser") {
		val statuser = listOf("VENTELISTE", "AKTUELL", "JATAKK", "INFOMOETE")

		createDeltakerProcessor().handleArenaMessage(createArenaDeltakerKafkaMessage(
			tiltakGjennomforingArenaId = ignoredGjennomforingArenaId,
			deltakerArenaId = 1,
			deltakerStatusKode = "AKTUELL"
		))
		verify { kafkaProducerService wasNot Called }

		statuser.forEachIndexed { idx, status ->
			createDeltakerProcessor().handleArenaMessage(createArenaDeltakerKafkaMessage(
				tiltakGjennomforingArenaId = nonIgnoredGjennomforingArenaId,
				deltakerArenaId = idx.toLong() + 1,
				deltakerStatusKode = status
			))
			getAndCheckArenaDataRepositoryEntry(operation = Operation.CREATED, (operationPos).toString())
		}
		verify(exactly = statuser.size) { kafkaProducerService.sendTilAktivitetskortTopic(any(), any(), any()) }
	}

	test("Insert Deltaker with gjennomføring not processed should throw exception") {
		shouldThrowExactly<DependencyNotIngestedException> {
			createDeltakerProcessor().handleArenaMessage(
				createArenaDeltakerKafkaMessage(
					2348790L,
					1L
				)
			)
		}
	}

	test("Should process deleted deltaker") {
		shouldThrowExactly<IgnoredException> {
			createDeltakerProcessor().handleArenaMessage(
				createArenaDeltakerKafkaMessage(
					tiltakGjennomforingArenaId = nonIgnoredGjennomforingArenaId,
					deltakerArenaId = 1L,
					operation = Operation.DELETED
				)
			)
		}
	}

	test("Skal opprette translation hvis regDato (opprettetTidspunkt) er innen en oppfølgingsperiode") {
		val opprettetTidspunkt = OppfolgingClientMock.defaultOppfolgingsperioder.last().startDato.toLocalDateTime().plusSeconds(10)
		val newDeltaker = createArenaDeltakerKafkaMessage(
			tiltakGjennomforingArenaId = nonIgnoredGjennomforingArenaId,
			deltakerArenaId = 1L,
			registrertDato = opprettetTidspunkt)
		createDeltakerProcessor().handleArenaMessage(newDeltaker)
		getAndCheckArenaDataRepositoryEntry(operation = Operation.CREATED, (operationPos).toString())
		val translationEntry = aktivitetRepository.getCurrentAktivitetsId(DeltakelseId(1), AktivitetKategori.TILTAKSAKTIVITET)
		translationEntry shouldNotBe null
	}

	test("Skal kaste OppfolgingsperiodeNotFoundException hvis ingen perioder ") {
		val oppfolgingsperioder = listOf<Oppfolgingsperiode>()
		val opprettetTidspunkt = LocalDateTime.now().minusDays(6)
		val newDeltaker = createArenaDeltakerKafkaMessage(
			tiltakGjennomforingArenaId = nonIgnoredGjennomforingArenaId,
			deltakerArenaId = 1L,
			registrertDato = opprettetTidspunkt)
		shouldThrowExactly<OppfolgingsperiodeNotFoundException> {
			createDeltakerProcessor(oppfolgingsperioder).handleArenaMessage(newDeltaker)
		}
	}

	test("Skal kaste IgnoredException hvis ingen passende perioder og eldre enn 1 uke") {
		val oppfolgingsperioder = listOf(
			Oppfolgingsperiode(
				uuid = UUID.randomUUID(),
				startDato = ZonedDateTime.now().minusMonths(2),
				sluttDato = ZonedDateTime.now().minusMonths(1)),
			Oppfolgingsperiode(
				uuid = UUID.randomUUID(),
				startDato = ZonedDateTime.now().minusWeeks(2),
				sluttDato = null)
		)
		val opprettetTidspunkt = LocalDateTime.now().minusMonths(3)
		val newDeltaker = createArenaDeltakerKafkaMessage(
			tiltakGjennomforingArenaId = nonIgnoredGjennomforingArenaId,
			deltakerArenaId = 1L,
			registrertDato = opprettetTidspunkt,
			endretTidspunkt = opprettetTidspunkt)
		shouldThrowExactly<IgnoredException> {
			createDeltakerProcessor(oppfolgingsperioder).handleArenaMessage(newDeltaker)
		}
	}

	test("skal permanent ignorere deltakelser som er avsluttet hvis de ikke har noen passende oppfølgingsperioder") {
		val oppfolgingsperioder = listOf(
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

		val opprettetTidspunkt = LocalDateTime.now().minusWeeks(3)

		val newDeltaker = createArenaDeltakerKafkaMessage(
			tiltakGjennomforingArenaId = nonIgnoredGjennomforingArenaId,
			deltakerArenaId = 1L,
			registrertDato = opprettetTidspunkt,
			endretTidspunkt = opprettetTidspunkt,
			deltakerStatusKode = "FULLF"
		)

		shouldThrowExactly<IgnoredException> {
			createDeltakerProcessor(oppfolgingsperioder).handleArenaMessage(newDeltaker)
		}
	}

	test("testing race condition in repo - not deltakerprocessor") {
		val firstTimeSlowAktivitetskortService = AktivitetService(slowAktivitetRepository, aktivitetskortIdRespository, deltakelseLockRepository)
		val aktivitetskortIdService = AktivitetskortIdService(aktivitetRepository, aktivitetskortIdRespository, deltakelseLockRepository)

		val deltakelse = DeltakelseId(12345L)
		val aktivitetskort1 = Aktivitetskort(
			id = UUID.randomUUID(),
			personIdent = "12345678901",
			tittel = "Tittel",
			aktivitetStatus = AktivitetStatus.GJENNOMFORES,
			etiketter = emptyList(),
			startDato = null,
			sluttDato = null,
			beskrivelse = null,
			endretAv = Ident("ARENAIDENT","AKS999"),
			endretTidspunkt = LocalDateTime.now(),
			avtaltMedNav = true,
			detaljer = emptyList()
		)
		val headers1 = AktivitetskortHeaders(
			arenaId = "ARENATA${deltakelse.value}",
			tiltakKode = "VASV",
			oppfolgingsperiode = UUID.randomUUID(),
			oppfolgingsSluttDato = ZonedDateTime.now().minusDays(5)
		)
		val aktivitetskort2 = aktivitetskort1.copy(id = UUID.randomUUID())
		val headers2 = headers1.copy( oppfolgingsperiode = UUID.randomUUID(), oppfolgingsSluttDato = null)

		coroutineScope {
			async(Dispatchers.IO) {// Do not use available default dispatcher, as it is not meant for blocking calls
				logger.info("First upsert start")
				firstTimeSlowAktivitetskortService.upsert(aktivitetskort1, headers1, deltakelse)
				logger.info("First upsert end")
			}
			async(Dispatchers.IO) {
				logger.info("Second upsert start")
				firstTimeSlowAktivitetskortService.upsert(aktivitetskort2, headers2, deltakelse)
				logger.info("Second upsert end")
			}
		}.await()
		val gimmeId = aktivitetskortIdService.getOrCreate(deltakelse, AktivitetKategori.TILTAKSAKTIVITET)
		gimmeId shouldBe aktivitetskort2.id // den uten oppfølgingsluttdato
	}

})
