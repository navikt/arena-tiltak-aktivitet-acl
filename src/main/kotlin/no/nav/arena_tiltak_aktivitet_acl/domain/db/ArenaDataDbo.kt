package no.nav.arena_tiltak_aktivitet_acl.domain.db

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import java.time.LocalDateTime

enum class IngestStatus {
	NEW,
	HANDLED,
	RETRY,
	FAILED,
	IGNORED,
	INVALID
}

data class ArenaDataDbo(
	val id: Int,
	val arenaTableName: String,
	val arenaId: String,
	val operation: Operation,
	val operationPosition: String,
	val operationTimestamp: LocalDateTime,
	val ingestStatus: IngestStatus,
	val ingestedTimestamp: LocalDateTime?,
	val ingestAttempts: Int = 0,
	val lastAttempted: LocalDateTime?,
	val before: String? = null,
	val after: String? = null,
	val note: String? = null
)
