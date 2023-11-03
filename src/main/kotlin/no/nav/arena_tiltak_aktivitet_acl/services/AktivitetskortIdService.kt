package no.nav.arena_tiltak_aktivitet_acl.services

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import no.nav.arena_tiltak_aktivitet_acl.repositories.AdvisoryLockRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetskortIdRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
open class AktivitetskortIdService(
	val aktivitetRepository: AktivitetRepository,
	val aktivitetskortIdRepository: AktivitetskortIdRepository,
	val advisoryLockRepository: AdvisoryLockRepository
) {
	/**
	 * SafeDeltakelse will make sure no other transaction is processing the same deltakelse for the duration of the ongoing transaction.
	 * If another transaction is processing the same deltakelse (i.e. AktivitetService) this transaction will wait its turn until the other transaction is complete.
	 * @see no.nav.arena_tiltak_aktivitet_acl.services.AktivitetService.upsert
	 */
	@Transactional
	open fun getOrCreate(deltakelseId: DeltakelseId, aktivitetKategori: AktivitetKategori): UUID {
		// Lock on deltakelseId. Gjelder så lenge den pågående transaksjonen er aktiv.
		advisoryLockRepository.safeDeltakelse(deltakelseId).use {
			val currentId = aktivitetRepository.getCurrentAktivitetsId(deltakelseId, aktivitetKategori)
			if (currentId != null) return currentId
			// Opprett i ny tabell
			return aktivitetskortIdRepository.getOrCreate(deltakelseId, aktivitetKategori)
		}
	}
}
