package no.nav.arena_tiltak_aktivitet_acl.repositories

import no.nav.arena_tiltak_aktivitet_acl.domain.db.DeltakerAktivitetMappingDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import no.nav.arena_tiltak_aktivitet_acl.utils.DatabaseUtils.sqlParameters
import no.nav.arena_tiltak_aktivitet_acl.utils.getUUID
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.IncorrectResultSizeDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.*

@Component
open class DeltakerAktivitetMappingRepository(
	private val template: NamedParameterJdbcTemplate
) {

	private val rowMapper = RowMapper { rs, _ ->
		DeltakerAktivitetMappingDbo(
			deltakelseId = DeltakelseId(rs.getLong("deltaker_id")),
			aktivitetId = rs.getUUID("aktivitet_id"),
			aktivitetKategori = AktivitetKategori.valueOf(rs.getString("aktivitet_kategori")),
			oppfolgingsperiodeUuid = rs.getUUID("oppfolgingsperiode_uuid")
		)
	}

	fun insert(entry: DeltakerAktivitetMappingDbo) {
		val insertNewAktivitet = """
			INSERT INTO deltaker_aktivitet_mapping(deltaker_id, aktivitet_id, aktivitet_kategori, oppfolgingsperiode_uuid, gjeldende)
			VALUES (:deltaker_id, :aktivitet_id, :aktivitet_kategori, :oppfolgingsperiode_uuid, true)
		""".trimIndent()

		val updateOldAktivitet = """
			UPDATE deltaker_aktivitet_mapping set gjeldende = false where deltaker_id = :deltaker_id and aktivitet_id != :aktivitet_id
		""".trimIndent()

		val updateParms = sqlParameters(
			"deltaker_id" to entry.deltakelseId.value,
			"aktivitet_id" to entry.aktivitetId )

		try {
			template.update(insertNewAktivitet, entry.asParameterSource())
			template.update(updateOldAktivitet, updateParms)
		} catch (e: DuplicateKeyException) {
			throw IllegalStateException("DeltakerAktivitetMapping entry on table with deltaker_id=${entry.deltakelseId}, aktivitet_id=${entry.aktivitetId}, aktivitet_kategori=${entry.aktivitetKategori} oppfolgingsperiode_uuid=${entry.oppfolgingsperiodeUuid} already exist.")
		}
	}


	fun get(deltakelseId: DeltakelseId, kategori: AktivitetKategori): List<DeltakerAktivitetMappingDbo> {
		val sql = """
			SELECT *
				FROM deltaker_aktivitet_mapping
				WHERE deltaker_id = :deltaker_id and aktivitet_kategori = :aktivitet_kategori
		""".trimIndent()

		val parameters = sqlParameters(
			"deltaker_id" to deltakelseId.value,
			"aktivitet_kategori" to kategori.name
		)
		return template.query(sql, parameters, rowMapper)
	}

	fun getCurrentAktivitetsId(deltakelseId: DeltakelseId, kategori: AktivitetKategori): UUID? {
		val sql = """
			SELECT aktivitet_id
				FROM deltaker_aktivitet_mapping
				WHERE deltaker_id = :deltaker_id and aktivitet_kategori = :aktivitet_kategori
				AND gjeldende = true
		""".trimIndent()

		val parameters = sqlParameters(
			"deltaker_id" to deltakelseId.value,
			"aktivitet_kategori" to kategori.name
		)
		val result = template.query(sql, parameters) { rs, _ ->
			rs.getUUID("aktivitet_id")
		}
		return when {
			result.isEmpty() -> null
			result.size > 1 -> throw IncorrectResultSizeDataAccessException(1, result.size)
			else -> result.first()
		}
	}

	private fun DeltakerAktivitetMappingDbo.asParameterSource() = sqlParameters(
		"deltaker_id" to deltakelseId.value,
		"aktivitet_id" to aktivitetId,
		"aktivitet_kategori" to aktivitetKategori.name,
		"oppfolgingsperiode_uuid" to oppfolgingsperiodeUuid
	)

}

