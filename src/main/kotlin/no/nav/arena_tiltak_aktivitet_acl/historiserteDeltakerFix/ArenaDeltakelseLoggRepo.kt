package no.nav.arena_tiltak_aktivitet_acl.historiserteDeltakerFix

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.ResultSet

@Component
class ArenaDeltakelseLoggRepo(
	private val template: NamedParameterJdbcTemplate
) {
	fun getSlettemeldinger(input: String): List<ArenaDeltakelseLogg> {
		val query = """
			SELECT * FROM TILTAKDELTAKER_JN
			WHERE JN_OPERATION = DEL
		""".trimIndent()
		return template.query(query, mapOf("lol" to input)) { resultSet, _ -> resultSet.toJournalfoertArenaDeltakelse() }
	}
}

fun ResultSet.toJournalfoertArenaDeltakelse(): ArenaDeltakelseLogg {
	return ArenaDeltakelseLogg(
		JN_OPERATION = JnOperation.valueOf(this.getString("JN_OPERATION")),
		JN_ORACLE_USER = this.getString("JN_ORACLE_USER"),
		JN_DATETIME = this.getTimestamp("JN_DATETIME").toLocalDateTime(),
		JN_APPLN = this.getString("JN_APPLN"),
		JN_SESSION = this.getString("JN_SESSION"),
		JN_TIMESTAMP = getTimestamp("JN_TIMESTAMP").toLocalDateTime(),
		TILTAKDELTAKER_ID = this.getLong("TILTAKDELTAKER_ID"),
		PERSON_ID = this.getLong("PERSON_ID"),
		TILTAKGJENNOMFORING_ID = this.getLong("TILTAKGJENNOMFORING_ID"),
		DELTAKERSTATUSKODE = DeltakerStatusKode.valueOf(this.getString("DELTAKERSTATUSKODE")),
		DELTAKERTYPEKODE = this.getString("DELTAKERTYPEKODE"),
		BEGRUNNELSE_PRIORITERING = this.getString("BEGRUNNELSE_PRIORITERING"),
		REG_DATO = this.getTimestamp("REG_DATO").toLocalDateTime(),
		REG_USER = this.getString("REG_USER"),
		MOD_DATO = this.getTimestamp("MOD_DATO").toLocalDateTime(),
		MOD_USER = this.getString("MOD_USER"),
		DATO_FRA = this.getDate("DATO_FRA").toLocalDate(),
		DATO_TIL = this.getDate("DATO_TIL").toLocalDate(),
		PROSENT_DELTID = this.getInt("PROSENT_DELTID"),
		BRUKERID_STATUSENDRING = this.getString("BRUKERID_STATUSENDRING"),
		DATO_STATUSENDRING = this.getTimestamp("DATO_STATUSENDRING").toLocalDateTime(),
		AKTIVITET_ID = this.getLong("AKTIVITET_ID"),
		BRUKERID_ENDRING_PRIORITERING = this.getString("BRUKERID_ENDRING_PRIORITERING"),
		DATO_ENDRING_PRIORITERING = this.getTimestamp("DATO_ENDRING_PRIORITERING")?.toLocalDateTime(),

	)
}
