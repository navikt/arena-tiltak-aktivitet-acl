package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet

import java.time.LocalDate
import java.util.*

interface AktivitetEventData {
	val id: UUID
	val personIdent: String
	val tittel: String
	val startDato: LocalDate? // dobbelsjekk
	val sluttDato: LocalDate? //
	val beskrivelse: String? //alle, men annen oppførsel på tiltak(jobbklubb)
}
