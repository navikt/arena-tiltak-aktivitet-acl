package no.nav.arena_tiltak_aktivitet_acl.repositories
import java.time.LocalDate

data class GjennomforingDbo(
	val arenaId: Long,
	val tiltakKode: String,
	val arrangorVirksomhetsnummer: String?,
	var arrangorNavn: String?,
	val navn: String,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
	val status: String,
)
