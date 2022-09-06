package no.nav.arena_tiltak_aktivitet_acl.clients.amt_enhetsregister

interface EnhetsregisterClient {

	fun hentVirksomhet(organisasjonsnummer: String): Virksomhet?

}
