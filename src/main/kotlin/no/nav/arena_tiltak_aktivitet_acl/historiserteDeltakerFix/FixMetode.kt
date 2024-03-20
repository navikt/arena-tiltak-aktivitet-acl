package no.nav.arena_tiltak_aktivitet_acl.historiserteDeltakerFix

import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataUpsertInput
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.OperationPos
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.ArenaDeltakelse
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import no.nav.arena_tiltak_aktivitet_acl.utils.asBackwardsFormattedLocalDateTime
import java.lang.IllegalStateException
import java.time.LocalDateTime
import java.util.*


sealed class FixMetode (val historiskDeltakelseId: Long, val deltakelseId: DeltakelseId) {
//	abstract fun toArenaDataUpsertInput(pos: OperationPos): ArenaDataUpsertInput?
}

class Oppdater(deltakelseId: DeltakelseId, val historiskDeltakelse: HistoriskDeltakelse, val generertPos: OperationPos): FixMetode(historiskDeltakelse.hist_tiltakdeltaker_id, deltakelseId) {
	fun toArenaDataUpsertInput(): ArenaDataUpsertInput {
		return historiskDeltakelseTilArenaDataUpsertInput(
			deltakelseId = deltakelseId,
			operation = Operation.DELETED,
			pos = generertPos,
			operationTimestamp = historiskDeltakelse.mod_dato.asBackwardsFormattedLocalDateTime(),
			before = historiskDeltakelse.toArenaDeltakelse(deltakelseId)
		)
	}
}

class OpprettMedLegacyId(deltakelseId: DeltakelseId, val historiskDeltakelse: HistoriskDeltakelse, val funksjonellId: UUID, val generertPos: OperationPos): FixMetode(historiskDeltakelse.hist_tiltakdeltaker_id, deltakelseId) {
	fun toArenaDataUpsertInput(): ArenaDataUpsertInput {
		return historiskDeltakelseTilArenaDataUpsertInput(
			deltakelseId = deltakelseId,
			operation = Operation.DELETED,
			pos = generertPos,
			operationTimestamp = historiskDeltakelse.mod_dato.asBackwardsFormattedLocalDateTime(),
			before = historiskDeltakelse.toArenaDeltakelse(deltakelseId)
		)
	}
}

class OppdaterTaptIACLMenFinnesIVeilarbaktivitet(deltakelseId: DeltakelseId, val historiskDeltakelse: HistoriskDeltakelse, val funksjonellId: UUID, val opType: String, val generertPos: OperationPos): FixMetode(historiskDeltakelse.hist_tiltakdeltaker_id, deltakelseId) {
	fun toArenaDataUpsertInput(): ArenaDataUpsertInput {
		val operation = opType.jnOperationToOperation()
		val content = historiskDeltakelse.toArenaDeltakelse(deltakelseId)
		return historiskDeltakelseTilArenaDataUpsertInput(
			deltakelseId = deltakelseId,
			operation = operation,
			pos = generertPos,
			operationTimestamp = historiskDeltakelse.mod_dato.asBackwardsFormattedLocalDateTime(),
			after = if (operation == Operation.DELETED) null else content,
			before = if (operation == Operation.CREATED) null else content
		)
	}
}

fun String.jnOperationToOperation(): Operation {
	return when {
		this == "DEL" -> Operation.DELETED
		this == "INS" -> Operation.CREATED
		this == "UPD" -> Operation.MODIFIED
		else -> throw IllegalStateException("Ikkje bra")
	}
}

class Opprett(deltakelseId: DeltakelseId, val historiskDeltakelse: HistoriskDeltakelse, val generertPos: OperationPos): FixMetode(historiskDeltakelse.hist_tiltakdeltaker_id, deltakelseId) {
	fun toArenaDataUpsertInput(): ArenaDataUpsertInput {
		return historiskDeltakelseTilArenaDataUpsertInput(
			deltakelseId = deltakelseId,
			operation = Operation.DELETED,
			pos = generertPos,
			operationTimestamp = historiskDeltakelse.mod_dato.asBackwardsFormattedLocalDateTime(),
			before = historiskDeltakelse.toArenaDeltakelse(deltakelseId)
		)
	}
}

fun historiskDeltakelseTilArenaDataUpsertInput(deltakelseId: DeltakelseId, operation: Operation, pos: OperationPos, operationTimestamp: LocalDateTime, before: ArenaDeltakelse?, after: ArenaDeltakelse? = null): ArenaDataUpsertInput {
	return ArenaDataUpsertInput(
		ArenaTableName.DELTAKER,
		arenaId = deltakelseId.toString(),
		operation = operation,
		operationPosition = pos,
		operationTimestamp = operationTimestamp,
		ingestStatus = IngestStatus.NEW,
		ingestedTimestamp = LocalDateTime.now(),
		before = before?.let { mapper.writeValueAsString(it) },
		after = after?.let { mapper.writeValueAsString(it) }
	)
}
