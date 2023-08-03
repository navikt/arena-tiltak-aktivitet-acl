package no.nav.arena_tiltak_aktivitet_acl.contoller

import no.nav.arena_tiltak_aktivitet_acl.domain.db.TranslationDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.repositories.TranslationRepository
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.lang.NumberFormatException

@RestController
@RequestMapping("/api")
class IdMappingController(
	val translationRepository: TranslationRepository
) {
	@GetMapping("id-mapping")
	fun getMapping(@RequestParam arenaId: String): TranslationDbo {
		return translationRepository.get(arenaId.toArenaId(), AktivitetKategori.TILTAKSAKTIVITET)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Ingen mapping på gitt arenaId")
	}
}

fun String.toArenaId(): Long {
	try {
		return this.removePrefix("ARENA")
			.removePrefix("TA")
			.toLong()
	} catch (e: NumberFormatException) {
		throw ResponseStatusException(HttpStatus.BAD_REQUEST)
	}
}
