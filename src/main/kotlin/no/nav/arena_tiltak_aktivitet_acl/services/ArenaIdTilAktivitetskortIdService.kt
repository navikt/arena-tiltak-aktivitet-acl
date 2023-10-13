package no.nav.arena_tiltak_aktivitet_acl.services

import no.nav.arena_tiltak_aktivitet_acl.domain.db.TranslationDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaIdTilAktivitetskortIdRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
open class ArenaIdTilAktivitetskortIdService(
	private val arenaIdTilAktivitetskortIdRepository: ArenaIdTilAktivitetskortIdRepository
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun setCurrentAktivitetskortIdForDeltakerId(deltakelseId: DeltakelseId, newAktivitetId: UUID) {
		log.info("Oppdaterer gjeldende aktivitetsId på deltakerId: $deltakelseId til $newAktivitetId")
		arenaIdTilAktivitetskortIdRepository.updateAktivitetId(deltakelseId, newAktivitetId)
	}

	fun hentAktivitetIdForArenaId(arenaId: DeltakelseId, aktivitetType: AktivitetKategori): UUID? {
		return arenaIdTilAktivitetskortIdRepository.get(arenaId, aktivitetType)?.aktivitetId
	}

	private fun insertTranslation(arenaId: DeltakelseId, aktivitetId: UUID, kategori: AktivitetKategori) {
		val translation = TranslationDbo(
			aktivitetId = aktivitetId,
			arenaId = arenaId,
			aktivitetKategori = kategori
		)

		arenaIdTilAktivitetskortIdRepository.insert(translation)
	}

	fun opprettAktivitetsId(nyAktivitetsId: UUID, deltakerArenaId: DeltakelseId, aktivitetType: AktivitetKategori): UUID {
		log.info("Opprettet ny aktivitetsid for deltakelse, aktivitetsId=$nyAktivitetsId deltakerId=$deltakerArenaId")
		insertTranslation(
			deltakerArenaId,
			nyAktivitetsId,
			aktivitetType
		)
		return nyAktivitetsId
	}

}
