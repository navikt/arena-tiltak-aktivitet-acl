package no.nav.arena_tiltak_aktivitet_acl.repositories

import java.util.*

data class TiltakDbo(
	val id: UUID,
	val kode: String,
	val navn: String,
	val administrasjonskode: String
)
