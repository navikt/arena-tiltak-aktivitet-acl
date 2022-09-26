package no.nav.arena_tiltak_aktivitet_acl.integration

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.*
import no.nav.arena_tiltak_aktivitet_acl.database.SingletonPostgresContainer
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetDbo
import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetRepository
import org.intellij.lang.annotations.Language
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

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
			data = "{}"
		)
		repository.upsert(aktivitet)
		repository.getAktivitet(id) shouldBe aktivitet
	}

	"upsert should not throw on duplicate key" {
		val aktivitet = AktivitetDbo(
			id = UUID.randomUUID(),
			personIdent = "123123123",
			kategori = AktivitetKategori.TILTAKSAKTIVITET,
			data = "{}"
		)
		repository.upsert(aktivitet)
		repository.upsert(aktivitet)
	}

	"upsert should update data on duplicate key" {
		val id = UUID.randomUUID()
		val aktivitet = AktivitetDbo(
			id = id,
			personIdent = "123123123",
			kategori = AktivitetKategori.TILTAKSAKTIVITET,
			data = "{}"
		)
		@Language("JSON")
		val updatedData = """{"data": "newData"}""".trimIndent()
		val updatedAktivtet = aktivitet.copy(data = updatedData)
		repository.upsert(aktivitet)
		repository.upsert(updatedAktivtet)
		repository.getAktivitet(id)?.data shouldBe updatedData
	}
})
