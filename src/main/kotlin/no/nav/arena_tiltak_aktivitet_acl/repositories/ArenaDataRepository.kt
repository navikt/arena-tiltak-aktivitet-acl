package no.nav.arena_tiltak_aktivitet_acl.repositories

import com.fasterxml.jackson.databind.JsonNode
import io.micrometer.core.annotation.Timed
import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataUpsertInput
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.dto.LogStatusCountDto
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.OperationPos
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import no.nav.arena_tiltak_aktivitet_acl.utils.DatabaseUtils.sqlParameters
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.time.LocalDateTime

@Component
open class ArenaDataRepository(
	private val template: NamedParameterJdbcTemplate,
) {

	private val rowMapper = RowMapper { rs, _ ->
		ArenaDataDbo(
			id = rs.getInt("id"),
			arenaTableName = ArenaTableName.fromValue(rs.getString("arena_table_name")),
			arenaId = rs.getString("arena_id"),
			operation = Operation.valueOf(rs.getString("operation_type")),
			operationPosition = OperationPos.of(rs.getString("operation_pos")),
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
		//language=PostgreSQL
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
			sql, mapOf(
				"arena_table_name" to upsertData.arenaTableName.tableName,
				"arena_id" to upsertData.arenaId,
				"operation_type" to upsertData.operation.name,
				"operation_pos" to upsertData.operationPosition.value,
				"operation_timestamp" to upsertData.operationTimestamp,
				"ingest_status" to upsertData.ingestStatus.name,
				"ingested_timestamp" to upsertData.ingestedTimestamp,
				"before" to upsertData.before,
				"after" to upsertData.after,
				"note" to upsertData.note,
			)
		)
	}

	fun upsertTemp(upsertData: ArenaDataUpsertInput) {
		//language=PostgreSQL
		val sql = """
			INSERT INTO temp_arena_data(arena_table_name, arena_id, operation_type, operation_pos, operation_timestamp, ingest_status,
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
			sql, mapOf(
				"arena_table_name" to upsertData.arenaTableName.tableName,
				"arena_id" to upsertData.arenaId,
				"operation_type" to upsertData.operation.name,
				"operation_pos" to upsertData.operationPosition.value,
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
		//language=PostgreSQL
		val sql = """
			UPDATE arena_data
			SET ingest_status = :ingest_status, last_attempted = :last_attempted
			WHERE id = :id
		""".trimIndent()

		template.update(
			sql, sqlParameters(
				"ingest_status" to ingestStatus.name,
				"id" to id,
				"last_attempted" to LocalDateTime.now(),
			)
		)
	}

	fun updateIngestAttempts(id: Int, ingestAttempts: Int, note: String?) {
		//language=PostgreSQL
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

	fun get(tableName: ArenaTableName, operation: Operation, position: OperationPos): ArenaDataDbo {
		//language=PostgreSQL
		val sql = """
			SELECT *
			FROM arena_data
			WHERE arena_table_name = :arena_table_name
				AND operation_type = :operation_type
				AND operation_pos = :operation_pos
		""".trimIndent()

		val parameters = sqlParameters(
			"arena_table_name" to tableName.tableName,
			"operation_type" to operation.name,
			"operation_pos" to position.value,
		)

		return template.query(sql, parameters, rowMapper).firstOrNull()
			?: throw NoSuchElementException("Element from table ${tableName.name}, operation: $operation, position: $position does not exist")
	}

	fun getByIngestStatus(
		tableName: ArenaTableName,
		status: IngestStatus,
		fromPos: OperationPos,
		limit: Int = 500
	): List<ArenaDataDbo> {
		//language=PostgreSQL
		val sql = """
			SELECT *
			FROM arena_data
			WHERE ingest_status = :ingestStatus
			AND arena_table_name = :tableName
			AND operation_pos > :fromPos
			ORDER BY operation_pos ASC
			LIMIT :limit
		""".trimIndent()

		val parameters = sqlParameters(
			"ingestStatus" to status.name,
			"tableName" to tableName.tableName,
			"fromPos" to fromPos.value,
			"limit" to limit
		)
		val resultat = template.query(sql, parameters, rowMapper)
		return resultat
	}

	fun getStatusCount(): List<LogStatusCountDto> {
		//language=PostgreSQL
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
		//language=PostgreSQL
		val sql = """
			SELECT *
			FROM arena_data
		""".trimIndent()

		return template.query(sql, rowMapper)
	}

	fun deleteAllIgnoredData(): Int {
		//language=PostgreSQL
		val sql = """
			DELETE FROM arena_data WHERE ingest_status = 'IGNORED'
		""".trimIndent()

		return template.update(sql, EmptySqlParameterSource())
	}

	@Timed(value = "acl.query.hasUnhandledDeltakelse")
	fun hasUnhandledDeltakelse(deltakelseArenaId: Long): Boolean {
		//language=PostgreSQL
		val sql = """
			SELECT count(*) as antall FROM arena_data
			where arena_id = :arena_id
				AND arena_table_name = :deltakerTableName
				AND ingest_status != 'HANDLED'
				AND ingest_status != 'IGNORED'
		""".trimIndent()
		val params = sqlParameters(
			"arena_id" to deltakelseArenaId.toString(),
			"deltakerTableName" to ArenaTableName.DELTAKER.tableName
		)
		return template.queryForObject(sql, params) { a, _ -> a.getInt("antall") }
			?.let { it > 0 } ?: false
	}

	fun hasHandledDeltakelseWithLaterPos(deltakelseId: DeltakelseId, operationPos: OperationPos): Boolean {
		//language=PostgreSQL
		val sql = """
			SELECT count(*) as antallNyereMeldinger FROM arena_data
			where arena_id = :arena_id
				AND arena_table_name = :deltakerTableName
				AND ingest_status = 'HANDLED'
				AND operation_pos > :operationPos
		""".trimIndent()
		val params = sqlParameters(
			"arena_id" to deltakelseId.value.toString(),
			"deltakerTableName" to ArenaTableName.DELTAKER.tableName,
			"operationPos" to operationPos.value
		)
		return template.queryForObject(sql, params) { a, _ -> a.getInt("antallNyereMeldinger") }
			?.let { it > 0 } ?: false
	}

	fun moveQueueForward(arenaTableName: ArenaTableName): Int {
		// For en deltakelse (arena_id)
		// Sett QUEUED til RETRY kun hvis det ikke finnes noen RETRY eller FAILED
		// Alt som er RETRY eller FAILED er alltid først i køen

		//language=PostgreSQL
		val sql = """
			UPDATE arena_data a SET ingest_status = 'RETRY' WHERE a.arena_table_name = :arenaTableName AND a.operation_pos in (
				SELECT MIN(operation_pos) FROM arena_data a2 -- Kan ikke stole på at ID er riktig rekkefølge
				WHERE a2.arena_table_name = :arenaTableName AND ingest_status = 'QUEUED' AND NOT EXISTS(
					SELECT 1 FROM arena_data a3 WHERE a3.ingest_status in ('RETRY','FAILED') AND a3.arena_id = a2.arena_id AND a3.arena_table_name = :arenaTableName)
				GROUP BY arena_id
			)
		""".trimIndent()
		return template.update(sql, mapOf("arenaTableName" to arenaTableName.tableName))
	}

	fun alreadyProcessed(deltakelseArenaId: String, tableName: ArenaTableName, before: JsonNode?, after: JsonNode?): Boolean {
		val sql = """
			WITH latestRow AS (
				SELECT arena_id, MAX(id) latestId
				FROM arena_data WHERE
					arena_id = :arenaId
					AND arena_table_name = :tableName
				GROUP BY arena_id)
			SELECT EXISTS(
				SELECT 1
				FROM arena_data
				JOIN latestRow ON arena_data.id = latestRow.latestId
			${if (after != null) "AND after @> :after::jsonb" else "AND after IS NULL"}
        	${if (before != null) "AND before @> :before::jsonb" else "AND before IS NULL"}
			)
		""".trimIndent()
		return template.queryForObject(
			sql,
			mapOf("arenaId" to deltakelseArenaId, "tableName" to tableName.tableName, "before" to before?.toString(), "after" to after?.toString()),
		) { row: ResultSet, _ -> row.getBoolean(1) }
	}

	fun getMostRecentDeltakelse(deltakelseArenaId: DeltakelseId): ArenaDataDbo? {
		val sql = """
				SELECT DISTINCT ON (arena_data.arena_id) *
				FROM arena_data WHERE
					arena_id = :deltakelseId AND arena_table_name = 'SIAMO.TILTAKDELTAKER'
				ORDER BY arena_id, operation_pos;
		""".trimIndent()
		return template.queryForObject(sql, mapOf("deltakelseId" to deltakelseArenaId.value.toString()), rowMapper)
	}

}
