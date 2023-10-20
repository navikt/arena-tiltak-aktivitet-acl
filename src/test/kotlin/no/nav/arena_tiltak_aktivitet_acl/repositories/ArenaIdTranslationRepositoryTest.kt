package no.nav.arena_tiltak_aktivitet_acl.repositories

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.arena_tiltak_aktivitet_acl.database.DatabaseTestUtils
import no.nav.arena_tiltak_aktivitet_acl.database.SingletonPostgresContainer
import no.nav.arena_tiltak_aktivitet_acl.domain.db.DeltakerAktivitetMappingDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.*

class ArenaIdTranslationRepositoryTest : FunSpec({

	val dataSource = SingletonPostgresContainer.getDataSource()

	lateinit var repository: DeltakerAktivitetMappingRepository

	val testObject = DeltakerAktivitetMappingDbo(
		deltakelseId = DeltakelseId(123L),
		aktivitetId = UUID.randomUUID(),
		aktivitetKategori = AktivitetKategori.TILTAKSAKTIVITET,
		oppfolgingsperiodeUuid = UUID.randomUUID()
	)

	beforeEach {
		val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
		rootLogger.level = Level.WARN

		repository = DeltakerAktivitetMappingRepository(NamedParameterJdbcTemplate(dataSource))

		DatabaseTestUtils.cleanDatabase(dataSource)
	}

	test("Insert and get should return inserted object") {
		repository.insert(testObject)

		val stored = repository.getCurrentAktivitetsId(testObject.deltakelseId, AktivitetKategori.TILTAKSAKTIVITET)

		stored shouldNotBe null
		stored!! shouldBe testObject.aktivitetId
	}
})
