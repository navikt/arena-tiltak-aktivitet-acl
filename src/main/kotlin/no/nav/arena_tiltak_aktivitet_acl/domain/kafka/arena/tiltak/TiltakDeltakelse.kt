package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

data class TiltakDeltakelse(
	val tiltakdeltakelseId: DeltakelseId,
	val tiltakgjennomforingId: Long,
	val personId: Long,
	val datoFra: LocalDate?,
	val datoTil: LocalDate?,
	val deltakerStatusKode: String,
	val datoStatusendring: LocalDateTime?,
	val dagerPerUke: Int?,
	val prosentDeltid: Float?,
	val regDato: LocalDateTime,
	val innsokBegrunnelse: String?,
	val regUser: String?,
	val modUser: String?,
	val modDato: LocalDateTime,
	val arenaAktivitetId: Long
)

data class DeltakelseId(
	val value: Long = Random.nextLong(1, 10000)
) {
	override fun toString(): String {
		return value.toString()
	}
}
