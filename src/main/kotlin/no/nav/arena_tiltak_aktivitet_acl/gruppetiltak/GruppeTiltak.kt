package no.nav.arena_tiltak_aktivitet_acl.gruppetiltak

import no.nav.arena_tiltak_aktivitet_acl.domain.InternalDomainObject
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.*
import no.nav.arena_tiltak_aktivitet_acl.processors.AktivitetskortOppfolgingsperiode
import no.nav.arena_tiltak_aktivitet_acl.services.KafkaProducerService
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

typealias GruppeTiltakAktivitetId = Long

data class GruppeTiltak(
	val arenaAktivitetId: GruppeTiltakAktivitetId,
	val aktivitetstype: String,
	val tittel: String,
	val beskrivelse: String,
	val datoFra: LocalDate,
	val datoTil: LocalDate,
	val motePlan: List<GruppeMote>,
	val personIdent: String,
	val opprettetTid: LocalDateTime,
	val opprettetAv: String,
	val endretTid: LocalDateTime?,
	val endretAv: String?,
): InternalDomainObject {

	override fun toAktivitetskort(
		aktivitetId: UUID,
		erNy: Boolean,
		operation: Operation,
		): Aktivitetskort {
		return Aktivitetskort(
			id = aktivitetId,
			personIdent = this.personIdent,
			tittel = this.tittel,
			aktivitetStatus = ArenaGruppeTiltakConverter.toAktivitetStatus(this.datoFra, this.datoTil, operation),
			startDato = this.datoFra,
			sluttDato = this.datoTil,
			avtaltMedNav = true, // Arenatiltak er alltid Avtalt med NAV
			beskrivelse = if (this.beskrivelse != null) Beskrivelse(verdi = this.beskrivelse) else null,
			endretTidspunkt = if (erNy) this.opprettetTid else this.endretTid ?: throw IllegalArgumentException("Missing modDato"),
			endretAv = if (erNy) Ident(ident = this.opprettetAv ?: throw IllegalArgumentException("Missing regUser"))
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

	override fun toAktivitetskortHeaders(oppfolgingsperiode: AktivitetskortOppfolgingsperiode): AktivitetskortHeaders {
		return AktivitetskortHeaders(
			arenaId = this.getArenaIdWithPrefix(),
			tiltakKode = this.aktivitetstype,
			oppfolgingsperiode = oppfolgingsperiode.id,
			oppfolgingsSluttDato = oppfolgingsperiode.oppfolgingsSluttDato,
		)
	}

	override fun arenaId() = this.arenaAktivitetId
	override fun opprettet() = this.opprettetTid
}

data class GruppeMote(
	val fra: LocalDateTime,
	val til: LocalDateTime,
	val sted: String,
	val moteId: Long
)
