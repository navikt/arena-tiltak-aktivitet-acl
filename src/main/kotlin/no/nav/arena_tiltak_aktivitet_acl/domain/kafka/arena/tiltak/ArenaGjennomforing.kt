package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.util.nullifyStringWithOnlySpecialChars
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.util.redactNorwegianSSNs
import no.nav.arena_tiltak_aktivitet_acl.repositories.GjennomforingDbo
import java.time.LocalDate

data class ArenaGjennomforing(
	val arenaId: Long,
	val tiltakKode: String,
	val arbgivIdArrangor: Long?,
	val lokaltNavn: String?,
	val datoFra: LocalDate?,
	val datoTil: LocalDate?,
	val statusKode: String,
) {
	fun toDbo(
		virksomhetsnummer: String?,
		arrangorNavn: String?
	): GjennomforingDbo {
		var vasketLokaltNavn: String? = null
		if (lokaltNavn != null) {
			vasketLokaltNavn = lokaltNavn
				.redactNorwegianSSNs()
				.nullifyStringWithOnlySpecialChars()
		}
		return GjennomforingDbo(
			arenaId = arenaId,
			tiltakKode = tiltakKode,
			arrangorVirksomhetsnummer = virksomhetsnummer,
			arrangorNavn = arrangorNavn,
			navn = vasketLokaltNavn,
			startDato = datoFra,
			sluttDato = datoTil,
			status = statusKode,
		)
	}

}
