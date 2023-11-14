package no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaOperation
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import java.time.LocalDateTime

class OppdaterDeltakerCommand(
	val oldDeltakerData: DeltakerInput,
	val updatedDeltakerData: DeltakerInput
) : DeltakerCommand(updatedDeltakerData.tiltakDeltakelseId) {
	override fun toArenaKafkaMessageDto(pos: String): ArenaKafkaMessageDto =
		ArenaKafkaMessageDto(
			table = ArenaTableName.DELTAKER,
			opType = ArenaOperation.U.name,
			opTs = LocalDateTime.now().format(opTsFormatter),
			pos = pos,
			before = createPayload(oldDeltakerData),
			after = createPayload(updatedDeltakerData)
		)
}
