package no.nav.arena_tiltak_aktivitet_acl.domain.db

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import java.util.*

data class DeltakerAktivitetMappingDbo(
	val deltakerId: Long,
	val aktivitetId: UUID,
	val aktivitetKategori: AktivitetKategori,
	val oppfolgingsperiodeUuid: UUID
)
