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
	val label: String?,
	val verdi: String
)

data class Attributt(
	val label: String,
	val verdi: String
)

data class Oppgave(
	val ekstern: OppgaveLenke,
	val intern: OppgaveLenke,
)

data class Url(
	// Should probably be an env-variable instead?
	val baseUrl: String,
	val path: String,
	val params: Map<String, String>,
)
data class OppgaveLenke(
	val tekst: String,
	val substekst: String,
	val url: Url
)
data class HandlingsLenke(
	val tekst: String,
	val substekst: String,
	val url: Url
)
data class Aktivitetskort(
	override val id: UUID,
	val eksternReferanseId: Long,
	override val personIdent: String, // Må alltid være fnr/dnr
	override val tittel: String,
	override val aktivitetStatus: AktivitetStatus,
	override val startDato: LocalDate?,
	override val sluttDato: LocalDate?,
	override val beskrivelse: Beskrivelse?, // alle, men annen oppførsel på tiltak(jobbklubb)
	override val endretAv: Ident,
	override val endretTidspunkt: LocalDateTime,
	override val avtaltMedNav: Boolean,
	val handlinger: List<HandlingsLenke?> = emptyList(),
	val oppgave: Oppgave? = null,
	val detaljer: List<Attributt>
) : AktivitetOrderData {
	private val objectMapper = ObjectMapper.get()
	fun toDbo() = AktivitetDbo(
		id = id,
		personIdent = personIdent,
		kategori = AktivitetKategori.TILTAKSAKTIVITET,
		data = objectMapper.writeValueAsString(this)
	)
}

