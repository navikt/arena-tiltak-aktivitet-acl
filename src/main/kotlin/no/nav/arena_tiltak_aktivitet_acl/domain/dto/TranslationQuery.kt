package no.nav.arena_tiltak_aktivitet_acl.domain.dto

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori

data class TranslationQuery(
	val arenaId: Long,
	val aktivitetKategori: AktivitetKategori
)
