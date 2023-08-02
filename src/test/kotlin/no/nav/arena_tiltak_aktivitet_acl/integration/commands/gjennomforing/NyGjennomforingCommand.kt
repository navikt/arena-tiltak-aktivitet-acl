package no.nav.arena_tiltak_aktivitet_acl.integration.commands.gjennomforing

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaOperation
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import java.time.LocalDateTime

class NyGjennomforingCommand(
	private val input: GjennomforingInput
) : GjennomforingCommand(input.gjennomforingId) {

	override fun execute(
		position: String,
		executor: (wrapper: ArenaKafkaMessageDto) -> GjennomforingResult
	): GjennomforingResult {
		val wrapper = ArenaKafkaMessageDto(
			table = ArenaTableName.GJENNOMFORING,
			opType = ArenaOperation.I.name,
			opTs = LocalDateTime.now().format(opTsFormatter),
			pos = position,
			before = null,
			after = createPayload(input)
		)

		return executor.invoke(wrapper)
	}

}
