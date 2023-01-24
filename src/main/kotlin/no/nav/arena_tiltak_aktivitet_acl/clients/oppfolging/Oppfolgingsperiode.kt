package no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging

import java.time.ZonedDateTime
import java.util.*

data class Oppfolgingsperiode (
	val uuid: UUID,
	val startDato: ZonedDateTime,
	val sluttDato: ZonedDateTime?
)
