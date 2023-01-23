package no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging

import java.time.ZonedDateTime
import java.util.*

data class Oppfolgingsperiode (
	private val uuid: UUID? = null,
	private val startDato: ZonedDateTime? = null,
	private val sluttDato: ZonedDateTime? = null
)
