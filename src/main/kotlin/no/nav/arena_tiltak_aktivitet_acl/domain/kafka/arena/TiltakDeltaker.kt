package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Aktivitet
import no.nav.arena_tiltak_aktivitet_acl.processors.converters.ArenaDeltakerStatusConverter
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class TiltakDeltaker(
    val tiltakdeltakerId: Long,
    val tiltakgjennomforingId: Long,
    val personId: Long,
    val datoFra: LocalDate?,
    val datoTil: LocalDate?,
    val deltakerStatusKode: String,
    val datoStatusendring: LocalDateTime?,
    val dagerPerUke: Int?,
    val prosentDeltid: Float?,
    val regDato: LocalDateTime,
    val innsokBegrunnelse: String?
) {
	fun convertToAktivitet(
		aktivitetId: UUID,
		personIdent: String,
		aktivitetType: Aktivitet.Type,
		tiltakKode: String,
		arrangorNavn: String?,
		beskrivelse: String?,
		tiltakNavn: String
	): Aktivitet {
		val status = ArenaDeltakerStatusConverter
			.toAktivitetStatus(deltakerStatusKode)

		return Aktivitet(
			id = aktivitetId,
			personIdent = personIdent,
			tittel = tiltakNavn,
			type = aktivitetType,
			tiltakKode = tiltakKode,
			status = status,
			startDato = datoFra,
			sluttDato = datoTil,
			arrangorNavn = arrangorNavn,
			deltakelseProsent = prosentDeltid,
			dagerPerUke = dagerPerUke,
			beskrivelse = beskrivelse,//TODO: denne skal være lokaltiltaksnavn på jobbklubb
			registrertDato = regDato,
			statusEndretDato = datoStatusendring,
		)
	}
}
