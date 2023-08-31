package no.nav.arena_tiltak_aktivitet_acl.utils

import no.nav.arena_tiltak_aktivitet_acl.exceptions.ValidationException
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

fun String.asValidatedLocalDate(fieldName: String): LocalDate {
	try {
		return this.asLocalDate()
	} catch (e: DateTimeParseException) {
		throw ValidationException("$fieldName kan ikke parses til LocalDate ($this)")
	}
}

fun String.asValidatedLocalDateTime(fieldName: String): LocalDateTime {
	try {
		return this.asLocalDateTime()
	} catch (e: DateTimeParseException) {
		throw ValidationException("$fieldName kan ikke parses til LocalDateTime ($this)")
	}
}

const val arenaDateTimeFormat = "yyyy-MM-dd HH:mm:ss"
val arenaDateTimeFormatter = DateTimeFormatter.ofPattern(arenaDateTimeFormat)
const val arenaTimeFormat = "HH:mm:ss"
val arenaTimeFormatter = DateTimeFormatter.ofPattern(arenaTimeFormat)
fun String.asLocalDate(): LocalDate = LocalDate.parse(this, arenaDateTimeFormatter)
fun String.asLocalDateTime(): LocalDateTime = LocalDateTime.parse(this, arenaDateTimeFormatter)
fun String.removeNullCharacters(): String {
	return this
		.replace("\u0000", "")
		.replace("\\u0000", "")
}
fun LocalDate.toArenaFormat() = arenaDateTimeFormatter.format(this.atStartOfDay())
fun LocalDateTime.toArenaFormat() = arenaDateTimeFormatter.format(this)
fun LocalTime.toArenaFormat() = arenaTimeFormatter.format(this)

fun String?.asTime(): LocalTime {
	val log = LoggerFactory.getLogger(String::class.java)

	if (this == null) {
		return LocalTime.MIDNIGHT
	} else if (this.matches("\\d\\d:\\d\\d".toRegex())) {
		val split = this.split(":")
		return LocalTime.of(split[0].toInt(), split[1].toInt())
	} else if (this.matches("\\d\\d\\.\\d\\d".toRegex())) {
		val split = this.split(".")
		return LocalTime.of(split[0].toInt(), split[1].toInt())
	} else if (this.matches("\\d\\d\\d\\d".toRegex())) {
		val hour = this.substring(0, 2)
		val minutes = this.substring(2, 4)

		return LocalTime.of(hour.toInt(), minutes.toInt())
	}

	log.warn("Det er ikke implementert en handler for klokketid, pattern: $this")

	return LocalTime.MIDNIGHT
}

infix fun LocalDate?.withTime(time: LocalTime) = this?.let { LocalDateTime.of(this, time) }
