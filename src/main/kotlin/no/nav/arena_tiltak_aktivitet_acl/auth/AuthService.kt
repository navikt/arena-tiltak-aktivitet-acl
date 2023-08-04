package no.nav.arena_tiltak_aktivitet_acl.auth

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Service
open class AuthService(
	private val tokenValidationContextHolder: TokenValidationContextHolder
) {

	fun claims() = tokenValidationContextHolder.tokenValidationContext.getClaims(Issuer.AZURE_AD)
	open fun hentAzureIdTilInnloggetBruker(): UUID = claims()
		.getStringClaim("oid").let { UUID.fromString(it) }
		?: throw ResponseStatusException(
			HttpStatus.UNAUTHORIZED,
			"oid is missing"
		)

	open fun harM2MRolleIToken(): Boolean = claims()
		.getAsList("roles")
		.contains(M2M_ROLE)

	fun validerErM2MToken() {
		if(!erM2MToken() || !harM2MRolleIToken())
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Action only permitted by m2m token")
	}
	private fun hentSubjectClaim(): UUID = claims()
		.getStringClaim("sub").let { UUID.fromString(it.toString()) }
		?: throw ResponseStatusException(
			HttpStatus.UNAUTHORIZED,
			"Sub is missing"
		)
	private fun erM2MToken() = hentAzureIdTilInnloggetBruker() == hentSubjectClaim()
}
