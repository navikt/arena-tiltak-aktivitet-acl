package no.nav.arena_tiltak_aktivitet_acl.processors.converters

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.*
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.GruppeTiltak
import java.time.LocalDate
import java.util.*

object ArenaGruppeTiltakConverter {

	fun toAktivitetStatus(startDato: LocalDate?, sluttDato: LocalDate?, kafkaOperation: Operation): AktivitetStatus {
		val now = LocalDate.now();
		return if (kafkaOperation == Operation.DELETED) AktivitetStatus.AVBRUTT
		else if (startDato == null) AktivitetStatus.PLANLAGT
		else if (startDato.isBefore(now)) AktivitetStatus.PLANLAGT
		else if (sluttDato == null) AktivitetStatus.GJENNOMFORES
		else if (sluttDato.isAfter(now)) AktivitetStatus.GJENNOMFORES
		else AktivitetStatus.FULLFORT
	}



	fun convertToTiltaksaktivitet(
		gruppeTiltak: GruppeTiltak,
		kafkaOperation: Operation,
		aktivitetId: UUID,
		personIdent: String,
		nyAktivitet: Boolean,
	): Aktivitetskort {
		return Aktivitetskort(
			id = aktivitetId,
			personIdent = personIdent,
			tittel = gruppeTiltak.aktivitetsnavn,
			aktivitetStatus = toAktivitetStatus(gruppeTiltak.datoFra, gruppeTiltak.datoTil, kafkaOperation),
			startDato = gruppeTiltak.datoFra,
			sluttDato = gruppeTiltak.datoTil,
			avtaltMedNav = true, // Arenatiltak er alltid Avtalt med NAV
			beskrivelse = if (gruppeTiltak.beskrivelse != null) Beskrivelse(verdi = gruppeTiltak.beskrivelse) else null,
			endretTidspunkt = if (nyAktivitet) gruppeTiltak.opprettetTid else gruppeTiltak.endretTid ?: throw IllegalArgumentException("Missing modDato"),
			endretAv = if (nyAktivitet) Ident(ident = gruppeTiltak.opprettetAv ?: throw IllegalArgumentException("Missing regUser"))
			           else Ident(ident = gruppeTiltak.endretAv ?: throw IllegalArgumentException("Missing modUser")),
			detaljer = listOfNotNull(
				if (gruppeTiltak.motePlan != null) Attributt("Tidspunkt og sted", "TODO hent fra møteplan") else null,
			),
			etiketter = listOf()
		)
	}
}
