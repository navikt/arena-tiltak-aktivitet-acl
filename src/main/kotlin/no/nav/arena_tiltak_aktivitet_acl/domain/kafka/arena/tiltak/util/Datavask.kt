package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.util

fun String.nullifyStringWithOnlySpecialChars(): String? {
	val specialCharsPattern = "^[\\p{P}\\p{S}\\p{Z}]+$".toRegex()
	return if (this.matches(specialCharsPattern)) {
		null
	} else {
		this
	}
}

fun String.redactNorwegianSSNs(): String {
	val ssnPattern = "\\b\\d{11}\\b|\\b\\d{6} \\d{5}\\b".toRegex()
	return this.replace(ssnPattern, "[FNR]")
}
