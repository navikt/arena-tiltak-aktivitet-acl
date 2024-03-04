package no.nav.arena_tiltak_aktivitet_acl.integration

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.nav.arena_tiltak_aktivitet_acl.database.SingletonPostgresContainer
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetDbo
import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetRepository
import org.intellij.lang.annotations.Language
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.ZonedDateTime
import java.util.*

class AktivitetRepositoryIntegrationTest: StringSpec({
	val dataSource = SingletonPostgresContainer.getDataSource()
	val template = NamedParameterJdbcTemplate(dataSource)
	val repository = AktivitetRepository(template)

	"upsert once should save data" {
		val id = UUID.randomUUID()
		val aktivitet = AktivitetDbo(
			id = id,
			personIdent = "123123123",
			kategori = AktivitetKategori.TILTAKSAKTIVITET,
			data = "{}",
			arenaId = "ARENATA-111",
			tiltakKode = "MIDLONNTIL",
			oppfolgingsperiodeUUID = UUID.randomUUID(),
			oppfolgingsSluttTidspunkt = null,
		)
		repository.upsert(aktivitet)
		repository.getAktivitet(id) shouldBe aktivitet
	}

	"upsert should not throw on duplicate key" {
		val aktivitet = AktivitetDbo(
			id = UUID.randomUUID(),
			personIdent = "123123123",
			kategori = AktivitetKategori.TILTAKSAKTIVITET,
			data = "{}",
			arenaId = "ARENATA-112",
			tiltakKode = "MIDLONNTIL",
			oppfolgingsperiodeUUID = UUID.randomUUID(),
			oppfolgingsSluttTidspunkt = null,
		)
		repository.upsert(aktivitet)
		repository.upsert(aktivitet)
	}

	"upsert should throw on multiple open perioder" {
		val aktivitet = AktivitetDbo(
			id = UUID.randomUUID(),
			personIdent = "123123123",
			kategori = AktivitetKategori.TILTAKSAKTIVITET,
			data = "{}",
			arenaId = "ARENATA-114",
			tiltakKode = "MIDLONNTIL",
			oppfolgingsperiodeUUID = UUID.randomUUID(),
			oppfolgingsSluttTidspunkt = null)
		val nyAktivitetskortSammeDeltakelse = aktivitet.copy(id = UUID.randomUUID())
		repository.upsert(aktivitet)
		shouldThrow<DuplicateKeyException> {
			repository.upsert(nyAktivitetskortSammeDeltakelse)
		}
	}

	"should throw on multiple same periode + arenaId" {
		val aktivitet = AktivitetDbo(
			id = UUID.randomUUID(),
			personIdent = "123123123",
			kategori = AktivitetKategori.TILTAKSAKTIVITET,
			data = "{}",
			arenaId = "ARENATA-114",
			tiltakKode = "MIDLONNTIL",
			oppfolgingsperiodeUUID = UUID.randomUUID(),
			oppfolgingsSluttTidspunkt = ZonedDateTime.now())
		val nyAktivitetskortSammeDeltakelse = aktivitet.copy(id = UUID.randomUUID())
		repository.upsert(aktivitet)
		shouldThrow<DuplicateKeyException> {
			repository.upsert(nyAktivitetskortSammeDeltakelse)
		}
	}

	"upsert should not throw on same arenaId" {
		val aktivitet = AktivitetDbo(
			id = UUID.randomUUID(),
			personIdent = "123123123",
			kategori = AktivitetKategori.TILTAKSAKTIVITET,
			data = "{}",
			arenaId = "ARENATA-116",
			tiltakKode = "MIDLONNTIL",
			oppfolgingsperiodeUUID = UUID.randomUUID(),
			oppfolgingsSluttTidspunkt = ZonedDateTime.now().minusDays(2))
		val nyAktivitetskortForskjelligPeriode = aktivitet.copy(
			id = UUID.randomUUID(),
			oppfolgingsperiodeUUID = UUID.randomUUID(),
			oppfolgingsSluttTidspunkt = null)
		repository.upsert(aktivitet)
		repository.upsert(nyAktivitetskortForskjelligPeriode)
	}

	"upsert should update data on duplicate key" {
		val id = UUID.randomUUID()
		val aktivitet = AktivitetDbo(
			id = id,
			personIdent = "123123123",
			kategori = AktivitetKategori.TILTAKSAKTIVITET,
			data = "{}",
			arenaId = "ARENATA-113",
			tiltakKode = "MIDLONNTIL",
			oppfolgingsperiodeUUID = UUID.randomUUID(),
			oppfolgingsSluttTidspunkt = null,
		)
		@Language("JSON")
		val updatedData = """{"data": "newData"}""".trimIndent()
		val updatedAktivtet = aktivitet.copy(data = updatedData)
		repository.upsert(aktivitet)
		repository.upsert(updatedAktivtet)
		repository.getAktivitet(id)?.data shouldBe updatedData
	}
})
