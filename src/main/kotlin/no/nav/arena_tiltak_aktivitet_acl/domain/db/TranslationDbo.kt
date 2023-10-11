package no.nav.arena_tiltak_aktivitet_acl.domain.db

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import java.util.*

data class TranslationDbo(
	val aktivitetId: UUID,
	val arenaId: DeltakelseId,
	val aktivitetKategori: AktivitetKategori
)
