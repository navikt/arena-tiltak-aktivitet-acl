package no.nav.arena_tiltak_aktivitet_acl.domain.db

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import java.util.*

data class TranslationDbo(
	val aktivitetId: UUID,
	val arenaId: Long,
	val aktivitetKategori: AktivitetKategori
)
