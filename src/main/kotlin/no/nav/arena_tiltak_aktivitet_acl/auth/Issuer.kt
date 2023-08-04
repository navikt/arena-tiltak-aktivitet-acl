package no.nav.arena_tiltak_aktivitet_acl.auth

object Issuer {
	// This name must match issuer configuration for tokens-support config
	// no.nav.security.jwt.issuer.<issuer-short-name-here>
	const val AZURE_AD = "azuread"
}

const val M2M_ROLE = "access_as_application"
