package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet

import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetDbo
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.arena_tiltak_aktivitet_acl.utils.ObjectMapper

data class Ident(
	val identType: String = "ARENAIDENT",
	val ident: String
)

data class Beskrivelse(
	val label: String? = null,
	val verdi: String
)

data class Attributt(
	val label: String,
	val verdi: String
)
data class Etikett(
	val kode: String,
)

data class Aktivitetskort(
	val id: UUID,
	val personIdent: String, // Må alltid være fnr/dnr
	val tittel: String,
	val aktivitetStatus: AktivitetStatus,
	val etiketter: List<Etikett>,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
	val beskrivelse: Beskrivelse?, // alle, men annen oppførsel på tiltak(jobbklubb)
	val endretAv: Ident,
	val endretTidspunkt: LocalDateTime,
	val avtaltMedNav: Boolean,
	val detaljer: List<Attributt>
) {
	private val objectMapper = ObjectMapper.get()
	fun toDbo() = AktivitetDbo(
		id = id,
		personIdent = personIdent,
		kategori = AktivitetKategori.TILTAKSAKTIVITET,
		data = objectMapper.writeValueAsString(this)
	)
}

