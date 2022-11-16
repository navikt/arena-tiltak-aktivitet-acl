package no.nav.arena_tiltak_aktivitet_acl.repositories

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori

class PersonTranslationDbo(
	val personIdent: Long,
	val fodselsnummer: String,
	val tiltakgjennomforingId: Long) {
}
