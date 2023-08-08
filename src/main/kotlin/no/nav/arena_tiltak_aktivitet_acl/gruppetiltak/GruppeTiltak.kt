package no.nav.arena_tiltak_aktivitet_acl.gruppetiltak

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.*
import no.nav.arena_tiltak_aktivitet_acl.services.KafkaProducerService
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
	val personIdent: String,
	val opprettetTid: LocalDateTime,
	val opprettetAv: String?,
	val endretTid: LocalDateTime?,
	val endretAv: String?,
) {
	fun convertToTiltaksaktivitet(
		kafkaOperation: Operation,
		aktivitetId: UUID,
		nyAktivitet: Boolean,
	): Aktivitetskort {
		return Aktivitetskort(
			id = aktivitetId,
			personIdent = this.personIdent,
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

	fun getArenaIdWithPrefix(): String {
		return KafkaProducerService.GRUPPE_TILTAK_ID_PREFIX + this.arenaAktivitetId.toString()
	}
}

data class GruppeMote(
	val fra: LocalDateTime,
	val til: LocalDateTime,
	val sted: String,
	val moteId: Long
)
