package no.nav.arena_tiltak_aktivitet_acl.processors.converters

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.amt.AmtDeltaker
import java.time.LocalDate

internal data class Status(
    val navn: AmtDeltaker.Status,
    val endretDato: LocalDate?
)
