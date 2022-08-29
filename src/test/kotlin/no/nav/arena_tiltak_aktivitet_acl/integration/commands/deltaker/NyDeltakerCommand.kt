package no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaOperation
import no.nav.arena_tiltak_aktivitet_acl.utils.ARENA_DELTAKER_TABLE_NAME
import java.time.LocalDateTime

class NyDeltakerCommand(private val input: DeltakerInput) : DeltakerCommand() {

	override fun execute(position: String, executor: (wrapper: ArenaKafkaMessageDto) -> AktivitetResult): AktivitetResult {
		val wrapper = ArenaKafkaMessageDto(
			table = ARENA_DELTAKER_TABLE_NAME,
			opType = ArenaOperation.I.name,
			opTs = LocalDateTime.now().format(opTsFormatter),
			pos = position,
			before = null,
			after = createPayload(input)
		)

		return executor.invoke(wrapper)
	}
}
