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

	fun hentEllerOpprettAktivitetId(arenaId: Long, aktivitetType: AktivitetKategori): UUID {
		val aktivitetId = translationRepository.get(arenaId, aktivitetType)?.aktivitetId
		if (aktivitetId != null) return aktivitetId
		val nyAktivitetsId = UUID.randomUUID()
		log.info("Opprettet ny id for deltaker/gruppedeltaker/utdanningsdeltaker, id=$nyAktivitetsId arenaId=$arenaId")
		insertTranslation(
			arenaId,
			nyAktivitetsId,
			aktivitetType
		)
		return nyAktivitetsId
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

}
