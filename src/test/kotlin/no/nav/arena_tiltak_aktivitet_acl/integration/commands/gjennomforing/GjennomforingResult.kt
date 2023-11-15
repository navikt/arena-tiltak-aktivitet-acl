package no.nav.arena_tiltak_aktivitet_acl.integration.commands.gjennomforing

import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.OperationPos
import no.nav.arena_tiltak_aktivitet_acl.repositories.GjennomforingDbo
import org.junit.jupiter.api.fail

data class GjennomforingResult(
	val position: OperationPos,
	val arenaDataDbo: ArenaDataDbo,
	val output: GjennomforingDbo?
) {

	fun arenaData(check: (data: ArenaDataDbo) -> Unit): GjennomforingResult {
		check.invoke(arenaDataDbo)
		return this
	}


	fun output(check: (data: GjennomforingDbo) -> Unit): GjennomforingResult {
		if (output == null) {
			fail("Trying to get output, but it is null")
		}

		check.invoke(output)
		return this
	}

	fun result(check: (arenaDataDbo: ArenaDataDbo, output: GjennomforingDbo?) -> Unit): GjennomforingResult {
		check.invoke(arenaDataDbo, output)
		return this
	}

}
