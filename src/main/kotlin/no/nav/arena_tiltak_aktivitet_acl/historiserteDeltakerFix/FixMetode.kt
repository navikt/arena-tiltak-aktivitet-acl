package no.nav.arena_tiltak_aktivitet_acl.historiserteDeltakerFix

import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataUpsertInput
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.OperationPos
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.ArenaDeltakelse
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import no.nav.arena_tiltak_aktivitet_acl.utils.asBackwardsFormattedLocalDateTime
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

fun historiskDeltakelseTilArenaDataUpsertInput(deltakelseId: DeltakelseId, operation: Operation, pos: OperationPos, operationTimestamp: LocalDateTime, before: ArenaDeltakelse?): ArenaDataUpsertInput {
	return ArenaDataUpsertInput(
		ArenaTableName.DELTAKER,
		arenaId = deltakelseId.toString(),
		operation = operation,
		operationPosition = pos,
		operationTimestamp = operationTimestamp,
		ingestStatus = IngestStatus.NEW,
		ingestedTimestamp = LocalDateTime.now(),
		before = before?.let { mapper.writeValueAsString(it) },
		after = null
	)
}
