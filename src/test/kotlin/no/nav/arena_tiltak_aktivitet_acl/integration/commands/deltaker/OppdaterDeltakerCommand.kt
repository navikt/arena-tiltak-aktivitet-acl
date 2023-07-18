package no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaOperation
import no.nav.arena_tiltak_aktivitet_acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import java.time.LocalDateTime

class OppdaterDeltakerCommand(
	val oldDeltakerData: DeltakerInput,
	val updatedDeltakerData: DeltakerInput
) : DeltakerCommand(updatedDeltakerData.tiltakDeltakerId) {

	override fun execute(position: String, executor: (wrapper: ArenaKafkaMessageDto) -> AktivitetResult): AktivitetResult {
		val wrapper = ArenaKafkaMessageDto(
			table = ArenaTableName.DELTAKER,
			opType = ArenaOperation.U.name,
			opTs = LocalDateTime.now().format(opTsFormatter),
			pos = position,
			before = createPayload(oldDeltakerData),
			after = createPayload(updatedDeltakerData)
		)

		return executor.invoke(wrapper)
	}

}
