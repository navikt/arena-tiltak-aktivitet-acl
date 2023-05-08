package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena

import java.time.LocalDate
import java.time.LocalDateTime

data class GruppeTiltak(
	val arenaAktivitetId: String,
	val aktivitetstype: String?,
	val aktivitetsnavn: String,
	val beskrivelse: String?,
	val datoFra: LocalDate?,
	val datoTil: LocalDate?,
	val motePlan: List<GruppeMote>?,
    val personId: Long?,
	val personIdent: String,
    val opprettetTid: LocalDateTime,
	val opprettetAv: String?,
	val endretTid: LocalDateTime?,
	val endretAv: String?,
)

data class GruppeMote(
	val tid: LocalDateTime?,
	val sted: String?
)
