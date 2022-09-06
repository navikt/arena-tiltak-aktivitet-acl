package no.nav.arena_tiltak_aktivitet_acl.clients.amt_enhetsregister

data class Virksomhet(
	val navn: String,
	val organisasjonsnummer: String,
	val overordnetEnhetOrganisasjonsnummer: String? = null,
	val overordnetEnhetNavn: String? = null,
)
