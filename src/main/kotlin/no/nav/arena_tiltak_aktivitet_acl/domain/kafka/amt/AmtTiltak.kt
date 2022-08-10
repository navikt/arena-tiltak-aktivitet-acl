package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.amt

import java.util.*

data class AmtTiltak(
	val id: UUID,
	val kode: String,
	val navn: String
)
