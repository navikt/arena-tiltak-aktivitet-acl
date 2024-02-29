package no.nav.arena_tiltak_aktivitet_acl.historiserteDeltakerFix

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
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
		UPDATE hist_tiltakdeltaker SET fix_metode = :fixMetode
		WHERE hist_tiltakdeltaker_id = :hist_tiltakdeltaker_id
	""".trimIndent()
		return template.update(query, mapOf("hist_tiltakdeltaker_id" to fixMetode.historiskDeltakelseId, "fixMetode" to fixMetode.javaClass.name))
	}

	data class DeltakelsePaaGjennomforing(
		val gjennomforingId: Long,
		val deltakelseId: DeltakelseId,
		val rekkefolge: Int
	)



	fun finnEksisterendeDeltakelserForGjennomforing(personId: Long, tiltakgjennomforingId: Long): List<DeltakelsePaaGjennomforing> {
		val query = """
			SELECT person_id, deltaker_id, gjennomforing_id, rekkefolge
			from deltaker_gjennomforing
			where person_id = :person_id
			and gjennomforing_id = :gjennomforing_id
			order by rekkefolge
		""".trimIndent()
		return template.query(query, mapOf("person_id" to personId, "gjennomforing_id" to tiltakgjennomforingId)) {
			resultSet, _ -> DeltakelsePaaGjennomforing(
				resultSet.getLong("gjennomforing_id"),
				DeltakelseId(resultSet.getLong("deltaker_id")),
				resultSet.getInt("rekkefolge")
				)
		}
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
