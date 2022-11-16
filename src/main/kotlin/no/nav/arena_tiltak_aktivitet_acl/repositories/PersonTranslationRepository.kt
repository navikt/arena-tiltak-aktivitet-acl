package no.nav.arena_tiltak_aktivitet_acl.repositories

import no.nav.arena_tiltak_aktivitet_acl.utils.DatabaseUtils
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
open class PersonTranslationRepository(
	private val template: NamedParameterJdbcTemplate
) {
	private val rowMapper = RowMapper { rs, _ ->
		PersonTranslationDbo(
			personIdent = rs.getLong("person_id"),
			fodselsnummer = rs.getString("fodselsnummer"),
			tiltakgjennomforingId = rs.getLong("tiltakgjennomforing_id")
		)
	}


	fun insert(entry: PersonTranslationDbo) {
		//language=PostgreSQL
		val sql = """
			INSERT INTO persontranslation(person_id, fodselsnummer, tiltakgjennomforing_id)
			VALUES (:person_id, :fodselsnummer, :tiltakgjennomforing_id)
			ON CONFLICT (person_id, tiltakgjennomforing_id) DO UPDATE SET fodselsnummer = :fodselsnummer
		""".trimIndent()

		template.update(sql, entry.asParameterSource())
	}

	fun get(personId: Long, gjennomforingId: Long): PersonTranslationDbo? {
		val sql = """
			SELECT *
				FROM persontranslation
				WHERE person_id = :person_id
				AND tiltakgjennomforing_id = :tiltakgjennomforing_id
		""".trimIndent()

		val parameters = DatabaseUtils.sqlParameters(
			"person_id" to personId,
			"tiltakgjennomforing_id" to gjennomforingId
		)

		return template.query(sql, parameters, rowMapper)
			.firstOrNull()
	}

	private fun PersonTranslationDbo.asParameterSource() = DatabaseUtils.sqlParameters(
		"person_id" to personIdent,
		"fodselsnummer" to fodselsnummer,
		"tiltakgjennomforing_id" to tiltakgjennomforingId
	)
}
