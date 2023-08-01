package no.nav.arena_tiltak_aktivitet_acl.rest

import no.nav.arena_tiltak_aktivitet_acl.auth.Issuer
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/translation")
class TranslationController {

	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@GetMapping("/arenaid")
	fun finnAktivitetsIdForArenaId(@RequestBody arenaId: String): String {
		// authService.validerErM2MToken()
		return hentAktivitetIdForArenaId(arenaId)
	}

	private fun hentAktivitetIdForArenaId(arenaId: String): String {
	throw NotImplementedError()
	}

}

