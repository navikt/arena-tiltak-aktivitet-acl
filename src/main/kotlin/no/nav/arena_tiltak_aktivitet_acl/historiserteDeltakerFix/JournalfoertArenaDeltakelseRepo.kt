package no.nav.arena_tiltak_aktivitet_acl.historiserteDeltakerFix

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.ResultSet

@Component
class JournalfoertArenaDeltakelseRepo(
	private val template: NamedParameterJdbcTemplate
) {
	fun get(input: String): List<JournalfoertArenaDeltakelse> {
		val query = """
			SELECT * FROM tablename
		""".trimIndent()
		return template.query(query, mapOf("lol" to input)) { resultSet, _ -> resultSet.toJournalfoertArenaDeltakelse() }
	}
}

fun ResultSet.toJournalfoertArenaDeltakelse(): JournalfoertArenaDeltakelse {
	return JournalfoertArenaDeltakelse(

	)
}
