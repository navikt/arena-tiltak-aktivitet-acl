package no.nav.arena_tiltak_aktivitet_acl.services

import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataTranslationDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Aktivitet
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataTranslationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
open class ArenaDataIdTranslationService(
	private val arenaDataIdTranslationRepository: ArenaDataTranslationRepository
) {

	private val log = LoggerFactory.getLogger(javaClass)



	fun hentEllerOpprettAktivitetId(deltakerArenaId: Long, aktivitetType: Aktivitet.Type): UUID {
		val deltakerId = arenaDataIdTranslationRepository.get(deltakerArenaId, aktivitetType)?.aktivitetId

		if (deltakerId == null) {
			val nyDeltakerIdId = UUID.randomUUID()
			log.info("Opprettet ny id for deltaker, id=$nyDeltakerIdId arenaId=$deltakerArenaId")
			return nyDeltakerIdId
		}

		return deltakerId
	}

	fun upsertTranslation(arenaId: Long, aktivitetId: UUID, aktivitetType: Aktivitet.Type) {
		val translation = ArenaDataTranslationDbo(
			aktivitetId = aktivitetId,
			arenaId = arenaId,
			aktivitetType = aktivitetType
		)

		arenaDataIdTranslationRepository.insert(translation)
	}

}
