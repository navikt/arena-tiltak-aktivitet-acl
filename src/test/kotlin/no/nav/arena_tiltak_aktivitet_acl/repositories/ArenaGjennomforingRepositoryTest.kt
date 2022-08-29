package no.nav.arena_tiltak_aktivitet_acl.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.date.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.arena_tiltak_aktivitet_acl.database.SingletonPostgresContainer
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDateTime
import kotlin.random.Random

class ArenaGjennomforingRepositoryTest : FunSpec({
	val datasource = SingletonPostgresContainer.getDataSource()
	lateinit var repository: GjennomforingRepository
	val now = LocalDateTime.now()
	beforeEach {
		repository = GjennomforingRepository(NamedParameterJdbcTemplate( datasource))
	}

	infix fun LocalDateTime.shouldBeCloseTo(other: LocalDateTime) {
		this shouldHaveSameYearAs other
		this shouldHaveSameMonthAs other
		this shouldHaveSameDayAs other
		this shouldHaveHour other.hour
		this shouldHaveMinute other.minute
	}

	test("upsert - skal inserte ny record") {
		val gjennomforing = GjennomforingDbo(
			arenaId = Random.nextLong(),
			tiltakKode = "INDOPPFAG",
			arrangorVirksomhetsnummer = "123",
			arrangorNavn = "Navn på arrangør",
			navn = "Gjennomføringnavn",
			startDato = now.toLocalDate(),
			sluttDato = now.toLocalDate().plusDays(1),
			status = "GJENNOMFOR",
		)
		repository.upsert(gjennomforing)

		val inserted = repository.get(gjennomforing.arenaId)
		inserted shouldNotBe null
		inserted!! shouldBe gjennomforing
	}

	test("upsert - kun obligatoriske felter - skal inserte ny record") {
		val gjennomforing = GjennomforingDbo(
			arenaId = Random.nextLong(),
			tiltakKode = "INDOPPFAG",
			arrangorVirksomhetsnummer = null,
			arrangorNavn = null,
			navn = "Gjennomføringnavn",
			startDato = null,
			sluttDato = null,
			status = "GJENNOMFOR"
		)
		repository.upsert(gjennomforing)

		val inserted = repository.get(gjennomforing.arenaId)

		inserted shouldNotBe null
		inserted!! shouldBe gjennomforing

	}
})
