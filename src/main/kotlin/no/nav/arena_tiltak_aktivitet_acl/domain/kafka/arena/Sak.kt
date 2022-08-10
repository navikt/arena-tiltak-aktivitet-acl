package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena


data class Sak(
	val sakId: Long,
	val sakskode: String,
	val aar: Int,
	val lopenr: Int,
	val ansvarligEnhetId: String,
)
