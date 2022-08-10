package no.nav.arena_tiltak_aktivitet_acl.domain.db

import java.util.*

data class ArenaDataIdTranslationDbo(
	val amtId: UUID,
	val arenaTableName: String,
	val arenaId: String,
	val ignored: Boolean,
)
