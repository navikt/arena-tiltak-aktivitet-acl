package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet

import java.time.LocalDateTime
import java.util.*

enum class Operation {
	CREATED,
	MODIFIED,
	DELETED;

	companion object {
		fun fromArenaOperationString(arenaOperationString: String): Operation {
			return when (arenaOperationString) {
				"I" -> CREATED
				"U" -> MODIFIED
				"D" -> DELETED
				else -> throw IllegalArgumentException("Unknown arena operation $arenaOperationString")
			}
		}
	}
}

enum class PayloadType {
	AKTIVITET
}

data class KafkaMessageDto<T>(
	val transactionId: UUID = UUID.randomUUID(),
	val source: String = "ARENA_TILTAK_AKTIVITET_ACL",
	val type: PayloadType,
	val timestamp: LocalDateTime = LocalDateTime.now(),
	val operation: Operation,
	val payload: T?
)
