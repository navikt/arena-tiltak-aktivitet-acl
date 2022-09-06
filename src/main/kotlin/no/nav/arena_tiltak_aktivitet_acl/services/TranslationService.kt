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
		val deltakerId = translationRepository.get(deltakerArenaId, aktivitetType)?.aktivitetId

		if (deltakerId == null) {
			val nyDeltakerIdId = UUID.randomUUID()
			log.info("Opprettet ny id for deltaker, id=$nyDeltakerIdId arenaId=$deltakerArenaId")
			return nyDeltakerIdId
		}

		return deltakerId
	}

	fun upsertTranslation(arenaId: Long, aktivitetId: UUID, kategori: AktivitetKategori) {
		val translation = TranslationDbo(
			aktivitetId = aktivitetId,
			arenaId = arenaId,
			aktivitetKategori = kategori
		)

		translationRepository.insert(translation)
	}

}
