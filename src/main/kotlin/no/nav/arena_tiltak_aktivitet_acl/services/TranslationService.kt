package no.nav.arena_tiltak_aktivitet_acl.services

import no.nav.arena_tiltak_aktivitet_acl.domain.db.TranslationDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.repositories.TranslationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
open class TranslationService(
	private val translationRepository: TranslationRepository
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun oppdaterAktivitetId(oldAktivitetId: UUID, newAktivitetId: UUID) {
		log.info("Oppdaterer gjeldende aktivitetsId fra $oldAktivitetId til $newAktivitetId")
		translationRepository.updateAktivitetId(oldAktivitetId, newAktivitetId)
	}

	fun hentAktivitetIdForArenaId(arenaId: Long, aktivitetType: AktivitetKategori): UUID? {
		return translationRepository.get(arenaId, aktivitetType)?.aktivitetId
	}

	private fun insertTranslation(arenaId: Long, aktivitetId: UUID, kategori: AktivitetKategori) {
		val translation = TranslationDbo(
			aktivitetId = aktivitetId,
			arenaId = arenaId,
			aktivitetKategori = kategori
		)

		translationRepository.insert(translation)
	}

	fun opprettAktivitetsId(deltakerArenaId: Long, aktivitetType: AktivitetKategori): UUID {
		val nyAktivitetsId = UUID.randomUUID()
		log.info("Opprettet ny aktivitetsid for deltakelse, aktivitetsId=$nyAktivitetsId deltakerId=$deltakerArenaId")
		insertTranslation(
			deltakerArenaId,
			nyAktivitetsId,
			aktivitetType
		)
		return nyAktivitetsId
	}

}
