package no.nav.arena_tiltak_aktivitet_acl.processors

import java.time.ZonedDateTime
import java.util.*

data class AktivitetskortOppfolgingsperiode(
    val id: UUID,
    val oppfolgingsSluttDato: ZonedDateTime?
)
