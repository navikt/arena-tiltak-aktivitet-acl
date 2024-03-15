package no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaOperation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.padUntil20Characters
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import java.time.LocalDateTime

class NyDeltakerCommand(private val input: DeltakerInput) : DeltakerCommand(input.tiltakDeltakelseId) {
	override fun toArenaKafkaMessageDto(pos: Long, operationTimestamp: LocalDateTime): ArenaKafkaMessageDto = ArenaKafkaMessageDto(
		table = ArenaTableName.DELTAKER,
		opType = ArenaOperation.I.name,
		opTs = operationTimestamp.format(opTsFormatter),
		pos = pos.padUntil20Characters(),
		before = null,
		after = createPayload(input)
	)
}
