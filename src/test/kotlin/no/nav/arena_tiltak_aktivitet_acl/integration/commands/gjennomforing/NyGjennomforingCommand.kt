package no.nav.arena_tiltak_aktivitet_acl.integration.commands.gjennomforing

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaOperation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.OperationPos
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import java.time.LocalDateTime

class NyGjennomforingCommand(
	private val input: GjennomforingInput
) : GjennomforingCommand(input.gjennomforingId) {

	override fun toArenaKafkaMessageDto(pos: String): ArenaKafkaMessageDto {
		return ArenaKafkaMessageDto(
			table = ArenaTableName.GJENNOMFORING,
			opType = ArenaOperation.I.name,
			opTs = LocalDateTime.now().format(opTsFormatter),
			pos = OperationPos.of(pos),
			before = null,
			after = createPayload(input)
		)
	}

}
