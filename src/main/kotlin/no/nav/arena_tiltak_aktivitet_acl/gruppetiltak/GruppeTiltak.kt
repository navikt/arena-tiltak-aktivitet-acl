package no.nav.arena_tiltak_aktivitet_acl.gruppetiltak

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class GruppeTiltak(
	val arenaAktivitetId: Long,
	val aktivitetstype: String,
	val aktivitetsnavn: String,
	val beskrivelse: String?,
	val datoFra: LocalDate?,
	val datoTil: LocalDate?,
	val motePlan: List<GruppeMote>?,
	val personId: Long?,
	val personIdent: String,
	val opprettetTid: LocalDateTime,
	val opprettetAv: String?,
	val endretTid: LocalDateTime?,
	val endretAv: String?,
) {
	fun convertToTiltaksaktivitet(
		kafkaOperation: Operation,
		aktivitetId: UUID,
		personIdent: String,
		nyAktivitet: Boolean,
	): Aktivitetskort {
		return Aktivitetskort(
			id = aktivitetId,
			personIdent = personIdent,
			tittel = this.aktivitetsnavn,
			aktivitetStatus = ArenaGruppeTiltakConverter.toAktivitetStatus(this.datoFra, this.datoTil, kafkaOperation),
			startDato = this.datoFra,
			sluttDato = this.datoTil,
			avtaltMedNav = true, // Arenatiltak er alltid Avtalt med NAV
			beskrivelse = if (this.beskrivelse != null) Beskrivelse(verdi = this.beskrivelse) else null,
			endretTidspunkt = if (nyAktivitet) this.opprettetTid else this.endretTid ?: throw IllegalArgumentException("Missing modDato"),
			endretAv = if (nyAktivitet) Ident(ident = this.opprettetAv ?: throw IllegalArgumentException("Missing regUser"))
			else Ident(ident = this.endretAv ?: throw IllegalArgumentException("Missing modUser")),
			detaljer = listOfNotNull(
				if (this.motePlan != null) Attributt("Tidspunkt og sted", "TODO hent fra møteplan") else null,
			),
			etiketter = listOf()
		)
	}
}

data class GruppeMote(
	val tid: LocalDateTime?,
	val sted: String?
)
