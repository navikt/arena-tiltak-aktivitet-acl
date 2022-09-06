package no.nav.arena_tiltak_aktivitet_acl.repositories

import no.nav.arena_tiltak_aktivitet_acl.utils.getUUID
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.*

@Component
open class TiltakRepository(
	private val template: NamedParameterJdbcTemplate
) {

	private val rowMapper = RowMapper { rs, _ ->
		TiltakDbo(
			id = rs.getUUID("id"),
			kode = rs.getString("kode"),
			navn = rs.getString("navn"),
			administrasjonskode = rs.getString("administrasjonskode")
		)
	}

	fun upsert(id: UUID, kode: String, navn: String, administrasjonskode: String) {
		val sql = """
			INSERT INTO arena_tiltak(id, kode, navn, administrasjonskode)
			VALUES (:id,
					:kode,
					:navn,
					:administrasjonskode)
			ON CONFLICT (kode) DO UPDATE SET navn = :navn
		""".trimIndent()

		val parameters = MapSqlParameterSource().addValues(
			mapOf(
				"id" to id,
				"kode" to kode,
				"navn" to navn,
				"administrasjonskode" to administrasjonskode
			)
		)

		template.update(sql, parameters)
	}


	fun getByKode(kode: String): TiltakDbo? {
		val sql = "SELECT * FROM arena_tiltak WHERE kode = :kode"

		return template.query(sql, singletonParameterMap("kode", kode), rowMapper).firstOrNull()
	}

	private fun singletonParameterMap(key: String, value: Any): MapSqlParameterSource {
		return MapSqlParameterSource().addValues(
			mapOf(
				key to value
			)
		)
	}
}
