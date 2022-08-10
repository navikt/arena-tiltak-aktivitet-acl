package no.nav.arena_tiltak_aktivitet_acl.integration.commands.sak
import kotlin.random.Random

data class SakInput (
	val sakId: Long = Random.nextLong(),
	val aar: Int = 2000,
	val lopenr: Int = Random.nextInt(),
	val ansvarligEnhetId: String = Random.nextInt().toString(),
	val sakKode: String = "TILT"
)
