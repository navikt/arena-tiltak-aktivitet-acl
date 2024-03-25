package no.nav.arena_tiltak_aktivitet_acl.domain.db

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaKafkaMessage
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.OperationPos
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import no.nav.arena_tiltak_aktivitet_acl.utils.ObjectMapper
import java.time.LocalDateTime

data class ArenaDataUpsertInput(
	val arenaTableName: ArenaTableName,
	val arenaId: String,
	val operation: Operation,
	val operationPosition: OperationPos,
	val operationTimestamp: LocalDateTime,
	val ingestStatus: IngestStatus = IngestStatus.NEW,
	val ingestedTimestamp: LocalDateTime? = null,
	val before: String? = null,
	val after: String? = null,
	val note: String? = null
)

private val objectMapper = ObjectMapper.get()

fun ArenaKafkaMessage<*>.toUpsertInput(arenaId: String, ingestStatus: IngestStatus, note: String? = null): ArenaDataUpsertInput {
	return ArenaDataUpsertInput(
		arenaTableName = this.arenaTableName,
		arenaId = arenaId,
		operation = this.operationType,
		operationPosition = this.operationPosition,
		operationTimestamp = this.operationTimestamp,
		ingestStatus = ingestStatus,
		ingestedTimestamp = LocalDateTime.now(),
		before = this.before?.let { objectMapper.writeValueAsString(it) },
		after = this.after?.let { objectMapper.writeValueAsString(it) },
		note = note
	)
}

fun ArenaKafkaMessage<*>.toUpsertInputWithStatusHandled(arenaId: Long): ArenaDataUpsertInput {
	return this.toUpsertInput(arenaId.toString(), IngestStatus.HANDLED, null)
}

fun ArenaKafkaMessage<*>.toUpsertInputWithStatusHandled(arenaId: DeltakelseId, note: String? = null): ArenaDataUpsertInput {
	return this.toUpsertInput(arenaId.value.toString(), IngestStatus.HANDLED, note)
}

fun ArenaKafkaMessage<*>.toUpsertInputWithStatusHandledAndIgnored(arenaId: DeltakelseId, note: String? = null): ArenaDataUpsertInput {
	return this.toUpsertInput(arenaId.value.toString(), IngestStatus.HANDLED_AND_IGNORED, note)
}

fun ArenaKafkaMessage<*>.toUpsertInputWithStatusHandled(arenaId: String): ArenaDataUpsertInput {
	return this.toUpsertInput(arenaId, IngestStatus.HANDLED, null)
}
