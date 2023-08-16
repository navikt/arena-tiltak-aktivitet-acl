package no.nav.arena_tiltak_aktivitet_acl.integration.commands

import no.nav.arena_tiltak_aktivitet_acl.utils.ObjectMapper
import java.time.format.DateTimeFormatter

abstract class Command (val key: String) {

	companion object {
		const val GENERIC_STRING = "STRING_NOT_SET"
		const val GENERIC_INT = Int.MIN_VALUE
		const val GENERIC_LONG = Long.MIN_VALUE
		const val GENERIC_FLOAT = Float.MIN_VALUE

		val opTsFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
		val dateFormatter = arenaFormatter

		val objectMapper = ObjectMapper.get()
	}

}

public val arenaFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

