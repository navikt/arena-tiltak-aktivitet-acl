package no.nav.arena_tiltak_aktivitet_acl.domain.dto

import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus

data class LogStatusCountDto(
	val status: IngestStatus,
	val count: Int
)
