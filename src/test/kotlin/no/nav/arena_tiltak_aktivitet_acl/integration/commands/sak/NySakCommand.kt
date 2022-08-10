package no.nav.arena_tiltak_aktivitet_acl.integration.commands.sak

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaOperation
import no.nav.arena_tiltak_aktivitet_acl.utils.ARENA_SAK_TABLE_NAME
import java.time.LocalDateTime

class NySakCommand(
	private val input: SakInput,
	private val gjennomforingId: Long
): SakCommand() {

	override fun execute(position: String, executor: (wrapper: ArenaKafkaMessageDto, gjennomforingId: Long) -> SakResult): SakResult {
		val wrapper = ArenaKafkaMessageDto(
			table = ARENA_SAK_TABLE_NAME,
			opType = ArenaOperation.I.name,
			opTs = LocalDateTime.now().format(opTsFormatter),
			pos = position,
			before = null,
			after = createPayload(input)
		)

		return executor.invoke(wrapper, gjennomforingId)
	}

}
