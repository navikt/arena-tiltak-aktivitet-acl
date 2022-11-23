package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena

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

		return GjennomforingDbo(
			arenaId = arenaId,
			tiltakKode = tiltakKode,
			arrangorVirksomhetsnummer = virksomhetsnummer,
			arrangorNavn = arrangorNavn,
			navn = lokaltNavn,
			startDato = datoFra,
			sluttDato = datoTil,
			status = statusKode,
		)
	}
}
