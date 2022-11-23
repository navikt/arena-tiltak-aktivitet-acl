package no.nav.arena_tiltak_aktivitet_acl.repositories

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.arena_tiltak_aktivitet_acl.database.DatabaseTestUtils
import no.nav.arena_tiltak_aktivitet_acl.database.SingletonPostgresContainer
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class PersonSporingRepositoryTest : FunSpec({
	val dataSource = SingletonPostgresContainer.getDataSource()

	lateinit var repository: PersonSporingRepository

	beforeEach {
		val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
		rootLogger.level = Level.WARN

		repository = PersonSporingRepository(NamedParameterJdbcTemplate(dataSource))

		DatabaseTestUtils.cleanDatabase(dataSource)
	}

	test("get - data finnes - skal hente inserta data") {

		val data = PersonSporingDbo(personIdent = 1234567, fodselsnummer = "10108031570", tiltakgjennomforingId = 1234)

		repository.upsert(data)

		val stored = repository.get(data.personIdent, data.tiltakgjennomforingId)

		stored shouldNotBe null
		stored?.personIdent shouldBe data.personIdent
		stored?.tiltakgjennomforingId shouldBe data.tiltakgjennomforingId
		stored?.fodselsnummer shouldBe data.fodselsnummer
	}

	test("upsert - data finnes allerede - oppdaterer eksisterende") {

		val data = PersonSporingDbo(personIdent = 1234567, fodselsnummer = "40108031570", tiltakgjennomforingId = 1234)

		repository.upsert(data)

		val stored = repository.get(data.personIdent, data.tiltakgjennomforingId)

		stored shouldNotBe null
		stored?.personIdent shouldBe data.personIdent
		stored?.tiltakgjennomforingId shouldBe data.tiltakgjennomforingId
		stored?.fodselsnummer shouldBe data.fodselsnummer

		val data2 = data.copy(fodselsnummer = "40108031570")

		repository.upsert(data2)

		val stored2 = repository.get(data.personIdent, data.tiltakgjennomforingId)

		stored2 shouldNotBe null
		stored2?.personIdent shouldBe data.personIdent
		stored2?.tiltakgjennomforingId shouldBe data.tiltakgjennomforingId
		stored2?.fodselsnummer shouldBe data2.fodselsnummer
	}
})
