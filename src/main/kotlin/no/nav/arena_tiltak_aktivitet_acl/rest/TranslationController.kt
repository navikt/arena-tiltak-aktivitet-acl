package no.nav.arena_tiltak_aktivitet_acl.rest

import no.nav.arena_tiltak_aktivitet_acl.auth.AuthService
import no.nav.arena_tiltak_aktivitet_acl.domain.dto.TranslationQuery
import no.nav.arena_tiltak_aktivitet_acl.services.TranslationService
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*


@RestController
@Protected
@RequestMapping("/api/translation")
class TranslationController(
	private val authService: AuthService,
	private val translationService: TranslationService
) {

	//@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@Protected
	@PostMapping("/arenaid")
	fun finnAktivitetsIdForArenaId(@RequestBody query: TranslationQuery): UUID? {
		authService.validerErM2MToken()
		return translationService.hentAktivitetIdForArenaId(query.arenaId, query.aktivitetKategori)
	}
}

