package no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging

import java.time.ZonedDateTime
import java.time.chrono.ChronoZonedDateTime
import java.util.*

data class Oppfolgingsperiode (
	val uuid: UUID,
	val startDato: ZonedDateTime,
	val sluttDato: ZonedDateTime?
) {
	fun contains(opprettet: ChronoZonedDateTime<*>): Boolean {
		val startetIPeriode = opprettet.isAfter(startDato) || opprettet.isEqual(startDato)
		val opprettetFørSluttDato = sluttDato == null || sluttDato.isAfter(opprettet)
		return startetIPeriode && opprettetFørSluttDato
	}
}
