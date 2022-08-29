package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet

import java.time.LocalDate
import java.time.LocalDateTime

//TODO: SLETT
data class Gjennomforing(
	val arenaId: Long,
	val tiltak: Tiltak,
	val virksomhetsnummer: String,
	val navn: String,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
	val registrertDato: LocalDateTime,
	val fremmoteDato: LocalDateTime?,
	val status: String,
)
