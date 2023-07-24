package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet

import java.time.ZonedDateTime
import java.util.*

data class AktivitetskortHeaders(
	val arenaId: String,
	val tiltakKode: String,
	val oppfolgingsperiode: UUID?,
	val oppfolgingsSluttDato: ZonedDateTime?,
	)

