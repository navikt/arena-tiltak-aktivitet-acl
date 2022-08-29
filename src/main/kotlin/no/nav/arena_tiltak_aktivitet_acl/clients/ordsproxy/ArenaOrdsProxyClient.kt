import no.nav.arena_tiltak_aktivitet_acl.clients.ordsproxy.Arbeidsgiver

interface ArenaOrdsProxyClient {

	fun hentFnr(arenaPersonId: Long): String?
	fun hentArbeidsgiver(arenaArbeidsgiverId: Long): Arbeidsgiver?
	fun hentVirksomhetsnummer(arenaArbeidsgiverId: Long): String
}
