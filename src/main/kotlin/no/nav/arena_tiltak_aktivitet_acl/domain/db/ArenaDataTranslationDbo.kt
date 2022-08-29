package no.nav.arena_tiltak_aktivitet_acl.domain.db

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Aktivitet
import java.util.*

data class ArenaDataTranslationDbo(
	val aktivitetId: UUID,
	val arenaId: Long,
	val aktivitetType: Aktivitet.Type
)
