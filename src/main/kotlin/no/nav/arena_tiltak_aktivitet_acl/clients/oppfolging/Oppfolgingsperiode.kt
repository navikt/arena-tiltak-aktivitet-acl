package no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging

import java.time.ZonedDateTime
import java.time.chrono.ChronoZonedDateTime
import java.util.*

data class Oppfolgingsperiode (
	val uuid: UUID,
	val startDato: ZonedDateTime,
	val sluttDato: ZonedDateTime?
) {
	fun tidspunktInnenforPeriode(tidspunkt: ChronoZonedDateTime<*>): Boolean {
		val startetIPeriode = tidspunkt.isAfter(startDato) || tidspunkt.isEqual(startDato)
		val foerSluttDato = sluttDato == null || sluttDato.isAfter(tidspunkt)
		return startetIPeriode && foerSluttDato
	}
}
