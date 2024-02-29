package no.nav.arena_tiltak_aktivitet_acl.historiserteDeltakerFix

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.ResultSet

@Component
class HistoriskDeltakelseRepo(
	private val template: NamedParameterJdbcTemplate
) {
	fun getHistoriskeDeltakelser(input: String): List<HistoriskDeltakelse> {
		val query = """
			SELECT * FROM hist_tiltakdeltaker order by person_id, tiltakgjennomforing_id, rekkefolge
		""".trimIndent()
		return template.query(query, mapOf("lol" to input)) { resultSet, _ -> resultSet.toHistoriskDeltakelse() }
	}
	fun oppdaterFixMetode(fixMetode: FixMetode) : Int {
		val query = """
		UPDATE TILTAKDELTAKER_JN SET FIX_METODE = :fixMetode
		WHERE TILTAKDELTAKER_ID = :tiltakdeltakerId and JN_OPERATION = 'DEL'
	""".trimIndent()
		return template.update(query, mapOf("tiltakdeltakerId" to fixMetode.deltakelseId.value, "fixMetode" to fixMetode.javaClass.name))
	}


}

fun ResultSet.toHistoriskDeltakelse(): HistoriskDeltakelse {
	return HistoriskDeltakelse(
		hist_tiltakdeltaker_id = this.getLong("hist_tiltakdeltaker_id"),
		person_id = this.getLong("person_id"),
			tiltakgjennomforing_id = this.getLong("tiltakgjennomforing_id"),
			deltakerstatuskode = this.getString("deltakerstatuskode"),
			deltakertypekode = this.getString("deltakertypekode"),
			aarsakverdikode_status = this.getString("aarsakverdikode_status"),
			oppmotetypekode = this.getString("oppmotetypekode"),
			prioritet = this.getString("prioritet"),
			prosent_deltid = this.getString("prosent_deltid"),
			brukerid_statusendring = this.getString("brukerid_statusendring"),
			dato_statusendring = this.getString("dato_statusendring"),
			dato_svarfrist = this.getString(""),
			dato_fra = this.getString("dato_fra"),
			dato_til = this.getString("dato_til"),
			aktivitet_id = this.getString("aktivitet_id"),
			reg_dato = this.getString("reg_dato"),
			reg_user = this.getString("reg_user"),
			mod_dato = this.getString("mod_dato"),
			mod_user = this.getString("mod_user"),
			brukerid_endring_prioritering = this.getString("brukerid_endring_prioritering"),
			dato_endring_prioritering = this.getString("dato_endring_prioritering"),
			dokumentkode_siste_brev = this.getString("dokumentkode_siste_brev"),
			rekkefolge = this.getInt("rekkefolge"),

	)
}
