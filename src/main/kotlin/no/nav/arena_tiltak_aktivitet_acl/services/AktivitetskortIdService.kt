package no.nav.arena_tiltak_aktivitet_acl.services

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetskortIdRepository
import java.util.*

class AktivitetskortIdService(
	val aktivitetRepository: AktivitetRepository,
	val aktivitetskortIdRepository: AktivitetskortIdRepository
) {
	fun getOrCreate(deltakelseId: DeltakelseId, aktivitetKategori: AktivitetKategori): UUID {
		val currentId = aktivitetRepository.getCurrentAktivitetsId(deltakelseId, aktivitetKategori)
		if (currentId != null) return currentId
		// Opprett i ny tabell
		return aktivitetskortIdRepository.getOrCreate(deltakelseId, aktivitetKategori)
	}
}
