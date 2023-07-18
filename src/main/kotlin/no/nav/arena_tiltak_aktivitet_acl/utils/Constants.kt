package no.nav.arena_tiltak_aktivitet_acl.utils

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

const val ARENA_TILTAK_TABLE_NAME = "SIAMO.TILTAK"
const val ARENA_GJENNOMFORING_TABLE_NAME = "SIAMO.TILTAKGJENNOMFORING"
const val ARENA_DELTAKER_TABLE_NAME = "SIAMO.TILTAKDELTAKER"

enum class ArenaTableName(val tableName: String) {
	DELTAKER(ARENA_DELTAKER_TABLE_NAME),
	GJENNOMFORING(ARENA_GJENNOMFORING_TABLE_NAME),
	TILTAK(ARENA_TILTAK_TABLE_NAME);

	companion object {
		@JsonCreator
		fun fromValue(tableName: String): ArenaTableName {
			return values().find { it.tableName == tableName }
				?: throw IllegalArgumentException("Invalid value: $tableName")
		}
	}

	@JsonValue
	override fun toString() = this.tableName
}

const val ONE_MINUTE = 60 * 1000L
const val ONE_HOUR = 60 * 60 * 1000L
const val AT_MIDNIGHT = "0 0 0 * * *"
