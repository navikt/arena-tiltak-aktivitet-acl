package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet

import java.util.*

data class Tiltak(
	val id: UUID,
	val kode: String, //TODO: Burde dette være en enum? Det er over 100 mulige verdier
	val navn: String,
	val administrasjonskode: Administrasjonskode
) {
	enum class Administrasjonskode {
		INST,
		IND,
		AMO
	}

}
