package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet

enum class AktivitetKategori(val prefix: String) {
	TILTAKSAKTIVITET("ARENATA"),
	UTDANNINGSAKTIVITET("ARENAUA"),
	GRUPPEAKTIVITET("ARENAGA")
}
