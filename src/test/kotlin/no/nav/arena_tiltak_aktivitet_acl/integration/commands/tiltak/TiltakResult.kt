package no.nav.arena_tiltak_aktivitet_acl.integration.commands.tiltak

import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Tiltak

data class TiltakResult(
	val arenaDataDbo: ArenaDataDbo,
	val tiltak: Tiltak
) {

	fun arenaData(check: (data: ArenaDataDbo) -> Unit): TiltakResult {
		check.invoke(arenaDataDbo)
		return this
	}

	fun tiltak(check: (data: Tiltak) -> Unit): TiltakResult {
		check.invoke(tiltak)
		return this
	}
}
