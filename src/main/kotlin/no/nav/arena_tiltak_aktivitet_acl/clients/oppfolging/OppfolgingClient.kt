package no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging

interface OppfolgingClient {

	fun hentOppfolgingsperioder(fnr: String): List<Oppfolgingsperiode>

}
