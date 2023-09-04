package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.util.redactNorwegianSSNs
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.util.replaceStringWithOnlySpecialChars
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
		var vasketArrangorNavn: String? = null
		if (arrangorNavn != null) {
			vasketArrangorNavn = arrangorNavn
				.redactNorwegianSSNs()
				.replaceStringWithOnlySpecialChars("Ukjent arrangør")
		}
		return GjennomforingDbo(
			arenaId = arenaId,
			tiltakKode = tiltakKode,
			arrangorVirksomhetsnummer = virksomhetsnummer,
			arrangorNavn = vasketArrangorNavn,
			navn = lokaltNavn,
			startDato = datoFra,
			sluttDato = datoTil,
			status = statusKode,
		)
	}

}
