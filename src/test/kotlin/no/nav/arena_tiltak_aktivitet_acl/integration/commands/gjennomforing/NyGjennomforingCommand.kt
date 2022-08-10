package no.nav.arena_tiltak_aktivitet_acl.integration.commands.gjennomforing

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaOperation
import no.nav.arena_tiltak_aktivitet_acl.utils.ARENA_GJENNOMFORING_TABLE_NAME
import java.time.LocalDateTime

class NyGjennomforingCommand(
	private val input: GjennomforingInput
) : GjennomforingCommand() {

	override fun execute(
		position: String,
		executor: (wrapper: ArenaKafkaMessageDto) -> GjennomforingResult
	): GjennomforingResult {
		val wrapper = ArenaKafkaMessageDto(
			table = ARENA_GJENNOMFORING_TABLE_NAME,
			opType = ArenaOperation.I.name,
			opTs = LocalDateTime.now().format(opTsFormatter),
			pos = position,
			before = null,
			after = createPayload(input)
		)

		return executor.invoke(wrapper)
	}

}
