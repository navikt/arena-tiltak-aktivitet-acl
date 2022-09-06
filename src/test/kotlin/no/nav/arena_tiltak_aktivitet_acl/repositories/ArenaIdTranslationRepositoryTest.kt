package no.nav.arena_tiltak_aktivitet_acl.repositories

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.arena_tiltak_aktivitet_acl.database.DatabaseTestUtils
import no.nav.arena_tiltak_aktivitet_acl.database.SingletonPostgresContainer
import no.nav.arena_tiltak_aktivitet_acl.domain.db.TranslationDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.*

class ArenaIdTranslationRepositoryTest : FunSpec({

	val dataSource = SingletonPostgresContainer.getDataSource()

	lateinit var repository: TranslationRepository

	val testObject = TranslationDbo(
		aktivitetId = UUID.randomUUID(),
		arenaId = 123L,
		aktivitetKategori = AktivitetKategori.TILTAKSAKTIVITET
	)

	beforeEach {
		val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
		rootLogger.level = Level.WARN

		repository = TranslationRepository(NamedParameterJdbcTemplate(dataSource))

		DatabaseTestUtils.cleanDatabase(dataSource)
	}

	test("Insert and get should return inserted object") {
		repository.insert(testObject)

		val stored = repository.get(testObject.arenaId, AktivitetKategori.TILTAKSAKTIVITET)

		stored shouldNotBe null
		stored!!.aktivitetId shouldBe testObject.aktivitetId
		stored.arenaId shouldBe testObject.arenaId
	}
})
