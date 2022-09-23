package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet

import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetDbo
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.arena_tiltak_aktivitet_acl.utils.ObjectMapper

data class Ident(
	val identType: String = "arenaIdent",
	val ident: String
)

data class TiltakAktivitet(
	override val id: UUID,
	val eksternReferanseId: Long,
	override val personIdent: String,
	override val tittel: String,
	override val aktivitetStatus: AktivitetStatus,
	override val startDato: LocalDate?,
	override val sluttDato: LocalDate?,
	override val beskrivelse: String?, // alle, men annen oppførsel på tiltak(jobbklubb)
	override val endretAv: Ident,
	override val endretTidspunkt: LocalDateTime,
	override val avtaltMedNav: Boolean,

	val tiltaksKode: String,
	val tiltaksNavn: String,
	val deltakelseStatus: DeltakelseStatus?,
	val arrangorNavn: String?,

	val detaljer: Map<String, String>
) : AktivitetOrderData {

	private val objectMapper = ObjectMapper.get()
	fun toDbo() = AktivitetDbo(
		id = id,
		personIdent = personIdent,
		kategori = AktivitetKategori.TILTAKSAKTIVITET,
		data = objectMapper.writeValueAsString(this)
	)
}

