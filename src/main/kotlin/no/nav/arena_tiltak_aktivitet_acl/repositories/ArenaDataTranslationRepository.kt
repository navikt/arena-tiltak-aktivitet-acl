package no.nav.arena_tiltak_aktivitet_acl.repositories

import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataTranslationDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Aktivitet
import no.nav.arena_tiltak_aktivitet_acl.utils.DatabaseUtils.sqlParameters
import no.nav.arena_tiltak_aktivitet_acl.utils.getUUID
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
open class ArenaDataTranslationRepository(
	private val template: NamedParameterJdbcTemplate
) {

	private val rowMapper = RowMapper { rs, _ ->
		ArenaDataTranslationDbo(
			aktivitetId = rs.getUUID("aktivitet_id"),
			arenaId = rs.getLong("arena_id"),
			aktivitetType = Aktivitet.Type.valueOf(rs.getString("aktivitet_type"))
		)
	}

	fun insert(entry: ArenaDataTranslationDbo) {
		val sql = """
			INSERT INTO arena_id_translation(aktivitet_id, arena_id, aktivitet_type)
			VALUES (:aktivitet_id, :arena_id, :aktivitet_type)
		""".trimIndent()

		try {
			template.update(sql, entry.asParameterSource())
		} catch (e: DuplicateKeyException) {
			throw IllegalStateException("Translation entry on table with id ${entry.arenaId} already exist.")
		}
	}

	fun get(arenaId: Long, aktivitetType: Aktivitet.Type): ArenaDataTranslationDbo? {
		val sql = """
			SELECT *
				FROM arena_id_translation
				WHERE arena_id = :arena_id
				AND aktivitet_type = :aktivitet_type
		""".trimIndent()

		val parameters = sqlParameters(
			"arena_id" to arenaId,
			"aktivitet_type" to aktivitetType.name
		)

		return template.query(sql, parameters, rowMapper)
			.firstOrNull()
	}

	private fun ArenaDataTranslationDbo.asParameterSource() = sqlParameters(
		"aktivitet_id" to aktivitetId,
		"arena_id" to arenaId,
		"aktivitet_type" to aktivitetType.name
	)

}

