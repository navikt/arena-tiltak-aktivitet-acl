package no.nav.arena_tiltak_aktivitet_acl.repositories

import no.nav.arena_tiltak_aktivitet_acl.utils.*
import no.nav.arena_tiltak_aktivitet_acl.utils.DatabaseUtils.sqlParameters
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class GjennomforingRepository (
	val template: NamedParameterJdbcTemplate
) {
	val rowMapper = RowMapper { rs, _ ->
		GjennomforingDbo(
			arenaId = rs.getLong("arena_id"),
			tiltakKode = rs.getString("tiltak_kode"),
			arrangorVirksomhetsnummer = rs.getString("arrangor_virksomhetsnummer"),
			arrangorNavn = rs.getString("arrangor_navn"),
			navn = rs.getString("navn"),
			startDato = rs.getNullableLocalDate("start_dato"),
			sluttDato = rs.getNullableLocalDate("slutt_dato"),
			status = rs.getString("status"),
		)
	}

	fun get(arenaId: Long) : GjennomforingDbo? {
		//language=PostgreSQL
		val sql = """
			SELECT * FROM gjennomforing where arena_id = :arena_id
		""".trimIndent()
		val params = sqlParameters("arena_id" to arenaId)
		return template.query(sql, params, rowMapper).firstOrNull()
	}

	fun upsert(gjennomforing: GjennomforingDbo) {
			//language=PostgreSQL
			val sql = """
				INSERT INTO gjennomforing (arena_id, tiltak_kode, arrangor_virksomhetsnummer, arrangor_navn, navn, start_dato, slutt_dato, status)
				VALUES (:arena_id, :tiltak_kode, :arrangor_virksomhetsnummer, :arrangor_navn, :navn, :start_dato, :slutt_dato, :status)
				ON CONFLICT(arena_id) DO UPDATE SET
					tiltak_kode = :tiltak_kode,
					arrangor_virksomhetsnummer = :arrangor_virksomhetsnummer,
					arrangor_navn = :arrangor_navn,
					navn = :navn,
					start_dato = :start_dato,
					slutt_dato = :slutt_dato,
					status = :status,
					modified_at = CURRENT_TIMESTAMP

			""".trimIndent()

		val params = sqlParameters(
				"arena_id" to gjennomforing.arenaId,
				"tiltak_kode" to gjennomforing.tiltakKode,
				"arrangor_virksomhetsnummer" to gjennomforing.arrangorVirksomhetsnummer,
				"arrangor_navn" to gjennomforing.arrangorNavn,
				"navn" to gjennomforing.navn,
				"start_dato" to gjennomforing.startDato,
				"slutt_dato" to gjennomforing.sluttDato,
				"status" to gjennomforing.status
			)

		template.update(sql, params)
	}
}
