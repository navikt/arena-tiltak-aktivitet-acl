package no.nav.arena_tiltak_aktivitet_acl.repositories

import no.nav.arena_tiltak_aktivitet_acl.domain.db.DeltakerAktivitetMappingDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.utils.DatabaseUtils.sqlParameters
import no.nav.arena_tiltak_aktivitet_acl.utils.getUUID
import org.springframework.dao.DuplicateKeyException
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
			oppfolgingsperiodeUuid = rs.getUUID("oppfolgingsperiode_uuid"),
		)
	}

	fun insert(entry: DeltakerAktivitetMappingDbo) {
		val sql = """
			INSERT INTO deltaker_aktivitet_mapping(deltaker_id, aktivitet_id, aktivitet_kategori, oppfolgingsperiode_uuid)
			VALUES (:deltaker_id, :aktivitet_id, :aktivitet_kategori, :oppfolgingsperiode_uuid)
		""".trimIndent()

		try {
			template.update(sql, entry.asParameterSource())
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

	private fun DeltakerAktivitetMappingDbo.asParameterSource() = sqlParameters(
		"deltaker_id" to deltakelseId.value,
		"aktivitet_id" to aktivitetId,
		"aktivitet_kategori" to aktivitetKategori.name,
		"oppfolgingsperiode_uuid" to oppfolgingsperiodeUuid
	)

}

typealias OppfolginsPeriodeId = UUID

