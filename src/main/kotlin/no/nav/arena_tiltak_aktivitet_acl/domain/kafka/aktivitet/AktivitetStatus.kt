package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet

enum class AktivitetStatus {
	BRUKER_ER_INTERESSERT, PLANLAGT, GJENNOMFORES, AVBRUTT, FULLFORT;

	fun erAvsluttet(): Boolean = listOf(AktivitetStatus.FULLFORT, AktivitetStatus.AVBRUTT).contains(this)
}
