package no.nav.arena_tiltak_aktivitet_acl.repositories

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori

import no.nav.arena_tiltak_aktivitet_acl.utils.getUUID
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
open class AktivitetRepository(
	private val template: NamedParameterJdbcTemplate
) {

	private val rowMapper = RowMapper { rs, _ ->
		AktivitetDbo(
			id = rs.getUUID("id"),
			personIdent = rs.getString("kode"),
			kategori = AktivitetKategori.valueOf(rs.getString("gruppe_type")),
			data = rs.getString("data")
		)
	}

	fun insert(aktivitet: AktivitetDbo) {
		val sql = """
			INSERT INTO aktivitet(id, person_ident, kategori_type, data)
			VALUES (:id,
					:person_ident,
					:kategori_type,
					:data::jsonb)
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
