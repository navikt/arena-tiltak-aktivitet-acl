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

	fun hentEllerOpprettAktivitetId(deltakerArenaId: Long, aktivitetType: AktivitetKategori): UUID {
		val aktivitetId = translationRepository.get(deltakerArenaId, aktivitetType)?.aktivitetId
		if (aktivitetId != null) return aktivitetId
		val nyAktivitetsId = UUID.randomUUID()
		log.info("Opprettet ny id for deltaker, id=$nyAktivitetsId arenaId=$deltakerArenaId")
		insertTranslation(
			deltakerArenaId,
			nyAktivitetsId,
			aktivitetType
		)
		return nyAktivitetsId
	}

	fun hentAktivitetIdForArenaId(arenaId: Long, aktivitetType: AktivitetKategori): UUID? {
		return translationRepository.get(arenaId, aktivitetType)?.aktivitetId
	}

	fun aktivitetIdExists(deltakerArenaId: Long, aktivitetType: AktivitetKategori): Boolean {
		val aktivitetId = translationRepository.get(deltakerArenaId, aktivitetType)?.aktivitetId

		return aktivitetId != null
	}

	private fun insertTranslation(arenaId: Long, aktivitetId: UUID, kategori: AktivitetKategori) {
		val translation = TranslationDbo(
			aktivitetId = aktivitetId,
			arenaId = arenaId,
			aktivitetKategori = kategori
		)

		translationRepository.insert(translation)
	}

}
