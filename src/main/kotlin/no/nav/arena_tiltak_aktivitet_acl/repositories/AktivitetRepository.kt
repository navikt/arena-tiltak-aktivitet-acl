package no.nav.arena_tiltak_aktivitet_acl.repositories

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori

import no.nav.arena_tiltak_aktivitet_acl.utils.getUUID
import org.intellij.lang.annotations.Language
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
open class AktivitetRepository(
	private val template: NamedParameterJdbcTemplate
) {
	fun upsert(aktivitet: AktivitetDbo) {
		val sql = """
			INSERT INTO aktivitet(id, person_ident, kategori_type, data)
			VALUES (:id,
					:person_ident,
					:kategori_type,
					:data::jsonb)
			ON CONFLICT ON CONSTRAINT aktivitet_pkey
			DO UPDATE SET data = :data::jsonb
		""".trimIndent()

		val parameters = MapSqlParameterSource().addValues(
			mapOf(
				"id" to aktivitet.id,
				"person_ident" to aktivitet.personIdent,
				"kategori_type" to aktivitet.kategori.name,
				"data" to aktivitet.data
			)
		)

		template.update(sql, parameters)
	}
}
