package no.nav.arena_tiltak_aktivitet_acl.repositories

import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataUpsertInput
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.dto.LogStatusCountDto
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.arena_tiltak_aktivitet_acl.utils.DatabaseUtils.sqlParameters
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
open class ArenaDataRepository(
	private val template: NamedParameterJdbcTemplate,
) {

	private val rowMapper = RowMapper { rs, _ ->
		ArenaDataDbo(
			id = rs.getInt("id"),
			arenaTableName = rs.getString("arena_table_name"),
			arenaId = rs.getString("arena_id"),
			operation = Operation.valueOf(rs.getString("operation_type")),
			operationPosition = rs.getString("operation_pos"),
			operationTimestamp = rs.getTimestamp("operation_timestamp").toLocalDateTime(),
			ingestStatus = IngestStatus.valueOf(rs.getString("ingest_status")),
			ingestedTimestamp = rs.getTimestamp("ingested_timestamp")?.toLocalDateTime(),
			ingestAttempts = rs.getInt("ingest_attempts"),
			lastAttempted = rs.getTimestamp("last_attempted")?.toLocalDateTime(),
			before = rs.getString("before"),
			after = rs.getString("after"),
			note = rs.getString("note")
		)
	}

	fun upsert(upsertData: ArenaDataUpsertInput) {
		val sql = """
			INSERT INTO arena_data(arena_table_name, arena_id, operation_type, operation_pos, operation_timestamp, ingest_status,
								   ingested_timestamp, before, after, note)
			VALUES (:arena_table_name,
					:arena_id,
					:operation_type,
					:operation_pos,
					:operation_timestamp,
					:ingest_status,
					:ingested_timestamp,
					:before::json,
					:after::json,
					:note)
			ON CONFLICT (arena_table_name, operation_type, operation_pos) DO UPDATE SET
					ingest_status      = :ingest_status,
					ingested_timestamp = :ingested_timestamp,
					note 			   = :note
		""".trimIndent()

		template.update(
			sql, sqlParameters(
				"arena_table_name" to upsertData.arenaTableName,
				"arena_id" to upsertData.arenaId,
				"operation_type" to upsertData.operation.name,
				"operation_pos" to upsertData.operationPosition,
				"operation_timestamp" to upsertData.operationTimestamp,
				"ingest_status" to upsertData.ingestStatus.name,
				"ingested_timestamp" to upsertData.ingestedTimestamp,
				"before" to upsertData.before,
				"after" to upsertData.after,
				"note" to upsertData.note,
			)
		)
	}

	fun updateIngestStatus(id: Int, ingestStatus: IngestStatus) {
		val sql = """
			UPDATE arena_data SET ingest_status = :ingest_status WHERE id = :id
		""".trimIndent()

		template.update(
			sql, sqlParameters(
				"ingest_status" to ingestStatus.name,
				"id" to id
			)
		)
	}

	fun updateIngestAttempts(id: Int, ingestAttempts: Int, note: String?) {
		val sql = """
			UPDATE arena_data SET
				ingest_attempts = :ingest_attempts,
			 	last_attempted = :last_attempted,
			 	note = :note
			  WHERE id = :id
		""".trimIndent()

		template.update(
			sql, sqlParameters(
				"ingest_attempts" to ingestAttempts,
				"last_attempted" to LocalDateTime.now(),
				"note" to note,
				"id" to id
			)
		)
	}

	fun get(tableName: String, operation: Operation, position: String): ArenaDataDbo {
		val sql = """
			SELECT *
			FROM arena_data
			WHERE arena_table_name = :arena_table_name
				AND operation_type = :operation_type
				AND operation_pos = :operation_pos
		""".trimIndent()

		val parameters = sqlParameters(
			"arena_table_name" to tableName,
			"operation_type" to operation.name,
			"operation_pos" to position,
		)

		return template.query(sql, parameters, rowMapper).firstOrNull()
			?: throw NoSuchElementException("Element from table $tableName, operation: $operation, position: $position does not exist")
	}

	fun getByIngestStatus(
		tableName: String,
		status: IngestStatus,
		fromId: Int,
		limit: Int = 500
	): List<ArenaDataDbo> {
		val sql = """
			SELECT *
			FROM arena_data
			WHERE ingest_status = :ingestStatus
			AND arena_table_name = :tableName
			AND id >= :fromId
			ORDER BY id, POS ASC
			LIMIT :limit
		""".trimIndent()

		val parameters = sqlParameters(
			"ingestStatus" to status.name,
			"tableName" to tableName,
			"fromId" to fromId,
			"limit" to limit
		)

		return template.query(sql, parameters, rowMapper)
	}

	fun getStatusCount(): List<LogStatusCountDto> {
		val sql = """
			SELECT ingest_status, count(*)
			FROM arena_data
			GROUP BY ingest_status
		""".trimIndent()

		val logRowMapper = RowMapper { rs, _ ->
			LogStatusCountDto(
				status = IngestStatus.valueOf(rs.getString("ingest_status")),
				count = rs.getInt("count")
			)
		}

		return template.query(sql, logRowMapper)
	}

	fun getAll(): List<ArenaDataDbo> {
		val sql = """
			SELECT *
			FROM arena_data
		""".trimIndent()

		return template.query(sql, rowMapper)
	}

	fun deleteAllIgnoredData(): Int {
		val sql = """
			DELETE FROM arena_data WHERE ingest_status = 'IGNORED'
		""".trimIndent()

		return template.update(sql, EmptySqlParameterSource())
	}

	fun hasUnhandledDeltakelse(deltakelseArenaId: Long): Boolean {
		//language=PostgreSQL
		val sql = """
			SELECT count(*) as antall FROM arena_data
			where arena_id = :arena_id
				AND arena_table_name = :deltakerTableName
				AND ingest_status != 'HANDLED'
		""".trimIndent()
		val params = sqlParameters(
			"arena_id" to deltakelseArenaId,
			"deltakerTableName" to ARENA_DELTAKER_TABLE_NAME
		)
		return template.queryForObject(sql, params) { a, _ -> a.getInt("antall") }
			?.let { it > 0 } ?: false
	}

	fun moveQueueForward() {
		//language=PostgreSQL
		val sql = """
			UPDATE arena_data a SET ingest_status = 'RETRY' WHERE a.id in (
				SELECT MIN(id) FROM arena_data a2
				WHERE ingest_status == 'QUEUED' AND NOT EXISTS(
					SELECT 1 FROM arena_data a3 WHERE a3.ingest_status != 'RETRY' AND a3.arena_id = a2.arena_id)
				GROUP BY arena_id
			)
		""".trimIndent()
		template.update(sql, MapSqlParameterSource())
	}

}
