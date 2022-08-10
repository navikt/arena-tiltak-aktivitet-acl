package no.nav.arena_tiltak_aktivitet_acl.domain.db

import java.time.ZonedDateTime

data class ArenaSakDbo(
	val id: Int,
	val arenaSakId: Long,
	val aar: Int,
	val lopenr: Int,
	val ansvarligEnhetId: String,
	val createdAt: ZonedDateTime
)
