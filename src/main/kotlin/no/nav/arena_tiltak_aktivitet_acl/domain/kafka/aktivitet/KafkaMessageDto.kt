package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet

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

enum class ActionType {
	UPSERT_AKTIVITETSKORT_V1,
}

enum class AktivitetskortType {
	ARENA_TILTAK,
	ARENA_UTDANNING,
	ARENA_GRUPPE
}

data class KafkaMessageDto(
	val messageId: UUID,
	val source: String = "ARENA_TILTAK_AKTIVITET_ACL",
	val actionType: ActionType,
	var aktivitetskortType: AktivitetskortType, // Arena tiltakskoder (105 forskjellige!)?
	val aktivitetskort: Aktivitetskort // f.eks UpsertTiltakAktivitetV1/TiltakAktivitet
)
