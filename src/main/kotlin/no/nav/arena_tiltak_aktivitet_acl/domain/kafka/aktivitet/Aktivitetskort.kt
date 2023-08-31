package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet

import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetDbo
import no.nav.arena_tiltak_aktivitet_acl.utils.ObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

const val ARENAIDENT = "ARENAIDENT"
data class Ident(
	val ident: String
) {
	val identType: String = ARENAIDENT
}

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
	fun toDbo(headers: AktivitetskortHeaders) = AktivitetDbo(
		id = id,
		personIdent = personIdent,
		kategori = AktivitetKategori.TILTAKSAKTIVITET,
		data = objectMapper.writeValueAsString(this),
		arenaId = headers.arenaId,
		tiltakKode = headers.tiltakKode,
		oppfolgingsperiodeUUID = headers.oppfolgingsperiode,
		oppfolgingsSluttTidspunkt = headers.oppfolgingsSluttDato
	)

	fun toKafkaMessage() = KafkaMessageDto(
			messageId = UUID.randomUUID(),
			actionType = ActionType.UPSERT_AKTIVITETSKORT_V1,
			aktivitetskort = this,
			aktivitetskortType = AktivitetskortType.ARENA_TILTAK
		)
}

