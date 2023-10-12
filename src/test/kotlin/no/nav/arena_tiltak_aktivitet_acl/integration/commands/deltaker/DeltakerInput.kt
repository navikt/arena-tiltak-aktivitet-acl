package no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Ident
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import kotlin.random.Random

data class DeltakerInput(
	val tiltakDeltakelseId: DeltakelseId,
	val tiltakgjennomforingId: Long,
	val personId: Long? = Random.nextLong(),
	val datoFra: LocalDate = LocalDate.now().minusDays(2),
	val datoTil: LocalDate = LocalDate.now().plusDays(2),
	val deltakerStatusKode: String = "GJENN",
	val datoStatusEndring: LocalDate = LocalDate.now().minusDays(2),
	val registrertDato: LocalDateTime = LocalDateTime.now().minusDays(7),
	val prosentDeltid: Float = 50.0f,
	val antallDagerPerUke: Int = 5,
	val innsokBegrunnelse: String? = null,
	val endretAv: Ident,
	val endretTidspunkt: LocalDateTime = LocalDateTime.now().minusDays(2)
)
