package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class Aktivitet(
	val id: UUID,
	val personIdent: String,
	val tittel: String, // Navn på aktiviteten, AMO-Kurs: Seilerkurs, Tiltak.tiltaksnavn + noe drit
	val type: Type,
	val tiltakKode: String, //Aktivitetkode i ram_aktivitet som også er lik tiltakskode i tiltaksgjennomføring INDOPPFAG
	val status: Status,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
	val arrangorNavn: String?, //TODO: required?
	val deltakelseProsent: Float?,
	val dagerPerUke: Int?,
	val beskrivelse: String?, //Jobbklubb=lokalttiltaksnavn, utdanning og gruppe
	val registrertDato: LocalDateTime,
	val statusEndretDato: LocalDateTime?,
) {

	enum class Type {
		TILTAKSAKTIVITET, UTDANNINGSAKTIVITET, GRUPPEAKTIVITET
	}

	enum class Status {
		BRUKER_ER_INTERESSERT, PLANLAGT, GJENNOMFORES, AVBRUTT, FULLFORT
	}
}
