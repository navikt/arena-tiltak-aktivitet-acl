package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import java.lang.IllegalArgumentException
import java.time.LocalDateTime

data class ArenaKafkaMessage<D>(
	val arenaTableName: ArenaTableName,
	val operationType: Operation,
	val operationTimestamp: LocalDateTime,
	val operationPosition: OperationPos,
	val before: D?,
	val after: D?
) {
	fun getData(): D {
		return when (operationType) {
			Operation.CREATED -> after ?: throw NoSuchElementException("Message with opType=CREATED is missing 'after'")
			Operation.MODIFIED -> after ?: throw NoSuchElementException("Message with opType=MODIFIED is missing 'after'")
			Operation.DELETED -> before ?: throw NoSuchElementException("Message with opType=DELETED is missing 'before'")
		}
	}
}

class OperationPos private constructor(val value: String) {
	companion object {
		fun of(posString: String): OperationPos = OperationPos(padUntil20Characters(posString))
	}
}

fun padUntil20Characters(stringValue: String): String {
	if (stringValue.toDoubleOrNull() == null) throw IllegalArgumentException("Operation-pos må være et tall")
	return stringValue.padStart(20 - stringValue.length, '0')
}
