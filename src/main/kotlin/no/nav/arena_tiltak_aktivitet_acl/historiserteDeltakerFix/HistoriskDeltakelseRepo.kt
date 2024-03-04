package no.nav.arena_tiltak_aktivitet_acl.historiserteDeltakerFix

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import no.nav.arena_tiltak_aktivitet_acl.utils.getLocalDateTime
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.*

@Component
class HistoriskDeltakelseRepo(
	private val template: NamedParameterJdbcTemplate
) {
	fun getHistoriskeDeltakelser(): List<HistoriskDeltakelse> {
		val query = """
			SELECT * FROM hist_tiltakdeltaker
			WHERE hist_tiltakdeltaker.fix_metode is null
			ORDER BY person_id, tiltakgjennomforing_id, rekkefolge
			LIMIT 1000
		""".trimIndent()
		return template.query(query) { resultSet, _ -> resultSet.toHistoriskDeltakelse() }
	}
	fun oppdaterFixMetode(fixMetode: FixMetode) : Int {
		val query = """
		UPDATE hist_tiltakdeltaker SET fix_metode = :fixMetode, generated_deltakerid = :generertDeltakerId, generated_pos = :generertPos
		WHERE hist_tiltakdeltaker_id = :hist_tiltakdeltaker_id
	""".trimIndent()
		val muligPos = when(fixMetode) {
			is Ignorer -> null
			is Oppdater -> fixMetode.generertPos
			is Opprett -> fixMetode.generertPos
			is OpprettMedLegacyId -> fixMetode.generertPos
		}
		val params =
			mapOf("hist_tiltakdeltaker_id" to fixMetode.historiskDeltakelseId,
				"fixMetode" to fixMetode.javaClass.name,
				"generertDeltakerId" to fixMetode.deltakelseId.value,
				"generertPos" to muligPos?.value)
		return template.update(query, params)
	}

	data class DeltakelsePaaGjennomforing(
		val gjennomforingId: Long,
		val deltakelseId: DeltakelseId,
		val rekkefolge: Int,
		val latestOperationPos: String,
		val latestModDato: LocalDateTime
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
				resultSet.getInt("rekkefolge"),
				resultSet.getString("latest_operation_pos"),
				resultSet.getLocalDateTime("latest_mod_dato")
				)
		}
	}

	fun getLegacyId(personId: Long, gjennomforingId: Long /*datoStatusEndring: String?*/): LegacyId? {
		val sql = """
			select translation.arena_id as deltakerId, translation.aktivitet_id as funksjonellId
			from hist_tiltakdeltaker
				join dobledeltakelser on hist_tiltakdeltaker.person_id = dobledeltakelser.person_id
				join translation on dobledeltakelser.tiltakdeltaker_id = translation.arena_id
				and hist_tiltakdeltaker.tiltakgjennomforing_id = dobledeltakelser.tiltakgjennomforing_id
				and dobledeltakelser.jn_operation = 'DEL'
				and dobledeltakelser.person_id = :person_id
				and dobledeltakelser.tiltakgjennomforing_id = :gjennomforing_id
				and to_timestamp(dobledeltakelser.dato_statusendring, 'YYYY-MM-DD HH24:MI:SS')
						= to_timestamp(hist_tiltakdeltaker.dato_statusendring, 'DD.MM.YYYY HH24:MI:SS');
		""".trimIndent()
		val params = mapOf(
			"person_id" to personId,
			"gjennomforing_id" to gjennomforingId,
//			"datoStatusEndring" to datoStatusEndring
		)
		return runCatching { template.queryForObject(sql, params)
			{ rs, _ -> LegacyId(UUID.fromString(rs.getString("funksjonellId")), DeltakelseId(rs.getLong("deltakerId"))) }
		}.getOrNull()
	}

	fun getNextFreeDeltakerId(min: Long, max: Long): DeltakelseId {
		// TODO: Dette funker ikke, altfor tregt
		val sql = """
			select *
			from generate_series(:min, :max) as ledig
			where ledig.ledig not in (
			    select arena_id::integer from arena_data where arena_table_name = 'SIAMO.TILTAKDELTAKER'
			    and arena_id::integer between 10000 AND 20000
			) limit 1;
		""".trimIndent()
		val params = mapOf("min" to min, "max" to max)
		return template.queryForObject(sql, params) { row, _ -> row.getLong(1) }
			.let { DeltakelseId(it) }
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

data class LegacyId(
	val funksjonellId: UUID,
	val deltakerId: DeltakelseId
)
