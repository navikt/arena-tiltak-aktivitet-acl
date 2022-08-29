package no.nav.arena_tiltak_aktivitet_acl.integration.commands.gjennomforing

import no.nav.arena_tiltak_aktivitet_acl.repositories.GjennomforingDbo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

data class GjennomforingInput(
	val tiltakKode: String = "INDOPPFAG",
	val gjennomforingId: Long = Random().nextLong(),
	val arbeidsgiverIdArrangor: Long? = 0,
	val navn: String? = "Tiltak hos Oslo kommune",
	val startDato: LocalDate = LocalDate.now().minusDays(7),
	val sluttDato: LocalDate = LocalDate.now().plusDays(7),
	val fremmoteDato: LocalDateTime = LocalDateTime.now().minusDays(7),
	val registrertDato: LocalDateTime = LocalDateTime.now().minusDays(14).truncatedTo(ChronoUnit.HOURS),
	val tiltakStatusKode: String = "GJENNOMFOR",
) {
	fun toDbo(arenaId: Long, virksomhetsnummer: String) = GjennomforingDbo(
		arenaId = arenaId,
		tiltakKode = tiltakKode,
		arrangorVirksomhetsnummer = virksomhetsnummer,
		arrangorNavn = "",
		navn = navn!!,
		startDato = startDato,
		sluttDato = sluttDato,
		status = tiltakStatusKode
	)
}
