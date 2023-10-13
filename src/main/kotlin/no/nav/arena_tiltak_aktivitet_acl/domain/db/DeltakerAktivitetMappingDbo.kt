package no.nav.arena_tiltak_aktivitet_acl.domain.db

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import java.util.*

data class DeltakerAktivitetMappingDbo(
	val deltakelseId: DeltakelseId,
	val aktivitetId: UUID,
	val aktivitetKategori: AktivitetKategori,
	val oppfolgingsperiodeUuid: UUID
)
