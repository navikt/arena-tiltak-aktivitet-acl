package no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.ZonedDateTime
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class OppfolgingsperiodeDto (
	val uuid: UUID,
	val startDato: ZonedDateTime,
	val sluttDato: ZonedDateTime?
)
