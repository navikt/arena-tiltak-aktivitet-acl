import no.nav.arena_tiltak_aktivitet_acl.clients.ordsproxy.Arbeidsgiver

interface ArenaOrdsProxyClient {

	fun hentFnr(arenaPersonId: String): String?
	fun hentArbeidsgiver(arenaArbeidsgiverId: String): Arbeidsgiver?
	fun hentVirksomhetsnummer(arenaArbeidsgiverId: String): String
}
