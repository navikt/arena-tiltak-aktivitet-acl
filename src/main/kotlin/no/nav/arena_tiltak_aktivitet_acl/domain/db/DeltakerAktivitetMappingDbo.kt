package no.nav.arena_tiltak_aktivitet_acl.domain.db

import java.util.*

data class DeltakerAktivitetMappingDbo(
	val deltakerId: Long,
	val aktivitetId: UUID,
	val oppfolgingsperiodeUuid: UUID
)
