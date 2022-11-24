package no.nav.arena_tiltak_aktivitet_acl.repositories

data class PersonSporingDbo(
	val personIdent: Long,
	val fodselsnummer: String,
	val tiltakgjennomforingId: Long) {
}
