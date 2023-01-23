package no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.ZonedDateTime
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class OppfolgingsperiodeDto (
	val uuid: UUID? = null,
	val startDato: ZonedDateTime? = null,
	val sluttDato: ZonedDateTime? = null
)
