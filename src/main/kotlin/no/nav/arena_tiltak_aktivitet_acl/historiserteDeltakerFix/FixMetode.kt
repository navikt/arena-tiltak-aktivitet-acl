package no.nav.arena_tiltak_aktivitet_acl.historiserteDeltakerFix

import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataUpsertInput
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.OperationPos
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.ArenaDeltakelse
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import java.time.LocalDateTime
import java.util.*


sealed class FixMetode (val deltakelseId: DeltakelseId) {
//	abstract fun toArenaDataUpsertInput(pos: OperationPos): ArenaDataUpsertInput?
}
class Ignorer(deltakelseId: DeltakelseId) : FixMetode(deltakelseId) {
//	override fun toArenaDataUpsertInput(pos: OperationPos): ArenaDataUpsertInput? = null
}

class Oppdater(deltakelseId: DeltakelseId, val arenaDeltakelse: ArenaDeltakelse, val historiskDeltakelse: HistoriskDeltakelse): FixMetode(deltakelseId) {
	fun toArenaDataUpsertInput(pos: OperationPos): ArenaDataUpsertInput {
		return arenaLoggTilArenaDataUpsertInput(
			deltakelseId = deltakelseId,
			operation = Operation.MODIFIED,
			pos = pos,
			operationTimestamp = LocalDateTime.MIN,
			before = mapper.writeValueAsString(arenaDeltakelse),
			after = mapper.writeValueAsString(historiskDeltakelse.toArenaDeltakelse(deltakelseId))
		)
	}
}

class OpprettMedLegacyId(deltakelseId: DeltakelseId, val historiskDeltakelse: HistoriskDeltakelse, val funksjonellId: UUID): FixMetode(deltakelseId) {
	fun toArenaDataUpsertInput(pos: OperationPos): ArenaDataUpsertInput {
		return arenaLoggTilArenaDataUpsertInput(
			deltakelseId = deltakelseId,
			operation = Operation.CREATED,
			pos = pos,
			operationTimestamp = LocalDateTime.MIN,
			before = null,
			after = mapper.writeValueAsString(historiskDeltakelse.toArenaDeltakelse(deltakelseId))
		)
	}
}

class Opprett(deltakelseId: DeltakelseId, val historiskDeltakelse: HistoriskDeltakelse): FixMetode(deltakelseId) {
	fun toArenaDataUpsertInput(pos: OperationPos): ArenaDataUpsertInput {
		return arenaLoggTilArenaDataUpsertInput(
			deltakelseId = deltakelseId,
			operation = Operation.CREATED,
			pos = pos,
			operationTimestamp = LocalDateTime.MIN,
			before = null,
			after = mapper.writeValueAsString(historiskDeltakelse.toArenaDeltakelse(deltakelseId))
		)
	}
}

fun arenaLoggTilArenaDataUpsertInput(deltakelseId: DeltakelseId, operation: Operation, pos: OperationPos, operationTimestamp: LocalDateTime, before: String?, after: String?): ArenaDataUpsertInput {
	return ArenaDataUpsertInput(
		ArenaTableName.DELTAKER,
		arenaId = deltakelseId.toString(),
		operation = operation,
		operationPosition = pos,
		operationTimestamp = operationTimestamp,
		ingestStatus = IngestStatus.NEW,
		ingestedTimestamp = LocalDateTime.now(),
		before = before,
		after = after
	)
}
