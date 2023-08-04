package no.nav.arena_tiltak_aktivitet_acl.integration.commands.tiltak

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Tiltak
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaOperation
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import java.time.LocalDateTime

class OppdaterTiltakCommand(
	val kode: String = "INDOPPFAG",
	val gammeltNavn: String,
	val nyttNavn: String,
	val administrasjonskode: Tiltak.Administrasjonskode = Tiltak.Administrasjonskode.IND
) : TiltakCommand(kode) {

	override fun toArenaKafkaMessageDto(pos: String): ArenaKafkaMessageDto = ArenaKafkaMessageDto(
		table = ArenaTableName.TILTAK,
		opType = ArenaOperation.U.name,
		opTs = LocalDateTime.now().format(opTsFormatter),
		pos = pos,
		before = createPayload(kode, gammeltNavn, administrasjonskode.name),
		after = createPayload(kode, nyttNavn, administrasjonskode.name)
	)
}
