package no.nav.arena_tiltak_aktivitet_acl.auth

import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

open class MockOAuthServer {

	private val azureAdIssuer = "azuread"
	private val tokenXIssuer = "tokenx"

	private val server = MockOAuth2Server()

	init {
		server.start()
		System.setProperty("MOCK_AZURE_AD_DISCOVERY_URL", server.wellKnownUrl(Issuer.AZURE_AD).toString())
		System.setProperty("MOCK_TOKEN_X_DISCOVERY_URL", server.wellKnownUrl(Issuer.TOKEN_X).toString())
	}

	fun shutdownMockServer() {
		server.shutdown()
	}

	fun issueAzureAdToken(
		subject: String = UUID.randomUUID().toString(),
		audience: String = "test-aud",
		ident: String,
		oid: UUID,
		adGroupIds: Array<String>,
	): String {
		val claims = mapOf(
			"NAVident" to ident,
			"oid" to oid.toString(),
			"groups" to adGroupIds,
		)

		return server.issueToken(azureAdIssuer, subject, audience, claims).serialize()
	}

	fun issueTokenXToken(
		ansattPersonIdent: String,
		subject: String = "test",
		audience: String = "test-aud",
	): String {

		val claims: Map<String, Any> = mapOf(
			"pid" to ansattPersonIdent
		)

		return server.issueToken(tokenXIssuer, subject, audience, claims).serialize()
	}

	fun issueAzureAdM2MToken(
		subject: String = UUID.randomUUID().toString(),
		audience: String = "test-aud",
		claims: Map<String, Any> = emptyMap()
	): String {
		val claimsWithRoles = claims.toMutableMap()
		claimsWithRoles["roles"] = arrayOf("access_as_application")
		claimsWithRoles["oid"] = subject
		return server.issueToken(azureAdIssuer, subject, audience, claimsWithRoles).serialize()
	}
}
