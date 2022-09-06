package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena

import java.time.LocalDate
import java.time.LocalDateTime

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
)
