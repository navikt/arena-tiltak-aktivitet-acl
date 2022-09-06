package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet

import java.util.*

data class Tiltak(
	val id: UUID,
	val kode: String,
	val navn: String,
	val administrasjonskode: Administrasjonskode
) {
	enum class Administrasjonskode {
		INST,
		IND,
		AMO
	}

}
