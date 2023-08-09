package no.nav.arena_tiltak_aktivitet_acl.domain.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori

data class TranslationQuery(
	@Schema(description = "Unik arenaid for en aktivitet og en aktivitetkategori", required = true)
	val arenaId: Long,
	@Schema(description = "Aktivitetskategori for arena-aktiviteter. En av TILTAKSAKTIVITET, UTDANNINGSAKTIVITET eller GRUPPEAKTIVITET", required = true)
	val aktivitetKategori: AktivitetKategori
)
