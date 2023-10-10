package no.nav.arena_tiltak_aktivitet_acl.repositories

import no.nav.arena_tiltak_aktivitet_acl.domain.db.DeltakerAktivitetMappingDbo
import no.nav.arena_tiltak_aktivitet_acl.utils.DatabaseUtils.sqlParameters
import no.nav.arena_tiltak_aktivitet_acl.utils.getUUID
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
open class DeltakerAktivitetMappingRepository(
	private val template: NamedParameterJdbcTemplate
) {

	private val rowMapper = RowMapper { rs, _ ->
		DeltakerAktivitetMappingDbo(
			deltakerId = rs.getLong("deltaker_id"),
			aktivitetId = rs.getUUID("aktivitet_id"),
			oppfolgingsperiodeUuid = rs.getUUID("oppfolgingsperiode_uuid"),
		)
	}

	fun insert(entry: DeltakerAktivitetMappingDbo) {
		val sql = """
			INSERT INTO deltaker_aktivitet_mapping(deltaker_id, aktivitet_id, oppfolgingsperiode_uuid)
			VALUES (:deltaker_id, :aktivitet_id, :oppfolgingsperiode_uuid)
		""".trimIndent()

		try {
			template.update(sql, entry.asParameterSource())
		} catch (e: DuplicateKeyException) {
			throw IllegalStateException("DeltakerAktivitetMapping entry on table with deltaker_id=${entry.deltakerId}, aktivitet_id=${entry.aktivitetId}, oppfolgingsperiode_uuid=${entry.oppfolgingsperiodeUuid} already exist.")
		}
	}


	fun get(deltakerId: Long): MutableList<DeltakerAktivitetMappingDbo> {
		val sql = """
			SELECT *
				FROM deltaker_aktivitet_mapping
				WHERE deltaker_id = :deltaker_id
		""".trimIndent()

		val parameters = sqlParameters(
			"deltaker_id" to deltakerId
		)
		return template.query(sql, parameters, rowMapper)
	}

	private fun DeltakerAktivitetMappingDbo.asParameterSource() = sqlParameters(
		"deltaker_id" to deltakerId,
		"aktivitet_id" to aktivitetId,
		"oppfolgingsperiode_uuid" to oppfolgingsperiodeUuid
	)

}

