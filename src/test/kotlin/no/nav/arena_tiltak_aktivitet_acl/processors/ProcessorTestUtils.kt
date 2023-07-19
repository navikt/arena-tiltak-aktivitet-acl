package no.nav.arena_tiltak_aktivitet_acl.processors

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaDeltaker
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaDeltakerKafkaMessage
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaTiltak
import no.nav.arena_tiltak_aktivitet_acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


var operationPos = 0L

private fun incrementAndGetPos(): Long {
	operationPos++
	return operationPos
}

fun createArenaDeltakerKafkaMessage(
	tiltakGjennomforingArenaId: Long,
	deltakerArenaId: Long,
	arenaPersonId: Long = 100L,
	oppstartDato: LocalDate? = null,
	sluttDato: LocalDate? = null,
	deltakerStatusKode: String = "GJENN",
	statusEndringDato: LocalDate? = null,
	dagerPerUke: Int? = null,
	prosentDeltid: Float = 0.0f,
	registrertDato: LocalDateTime = LocalDateTime.now(),
	endretTidspunkt: LocalDateTime = LocalDateTime.now(),
	operation: Operation = Operation.CREATED
): ArenaDeltakerKafkaMessage {
	val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

	val deltaker = emptyArenaTiltakDeltaker().copy(
		TILTAKGJENNOMFORING_ID = tiltakGjennomforingArenaId,
		TILTAKDELTAKER_ID = deltakerArenaId,
		PERSON_ID = arenaPersonId,
		DATO_FRA = oppstartDato?.format(formatter),
		DATO_TIL = sluttDato?.format(formatter),
		DELTAKERSTATUSKODE = deltakerStatusKode,
		DATO_STATUSENDRING = statusEndringDato?.format(formatter),
		ANTALL_DAGER_PR_UKE = dagerPerUke,
		PROSENT_DELTID = prosentDeltid,
		MOD_DATO = endretTidspunkt.format(formatter),
		REG_DATO = registrertDato.format(formatter)
	)

	return ArenaDeltakerKafkaMessage(
		arenaTableName = ArenaTableName.DELTAKER,
		operationType = operation,
		operationTimestamp = LocalDateTime.now(),
		operationPosition = incrementAndGetPos().toString(),
		after = if (operation != Operation.DELETED) deltaker else null,
		before = if (operation != Operation.CREATED) deltaker else null
	)
}

private fun emptyArenaTiltak(): ArenaTiltak {
	val NOT_SET_STRING = "NOT_SET_STRING"
	val NOT_SET_INT = Int.MIN_VALUE

	return ArenaTiltak(
		TILTAKSNAVN = NOT_SET_STRING,
		TILTAKSGRUPPEKODE = NOT_SET_STRING,
		REG_DATO = NOT_SET_STRING,
		REG_USER = NOT_SET_STRING,
		MOD_DATO = NOT_SET_STRING,
		MOD_USER = NOT_SET_STRING,
		TILTAKSKODE = NOT_SET_STRING,
		DATO_FRA = NOT_SET_STRING,
		DATO_TIL = NOT_SET_STRING,
		AVSNITT_ID_GENERELT = NOT_SET_INT,
		STATUS_BASISYTELSE = NOT_SET_STRING,
		ADMINISTRASJONKODE = NOT_SET_STRING,
		STATUS_KOPI_TILSAGN = NOT_SET_STRING,
		ARKIVNOKKEL = NOT_SET_STRING,
		STATUS_ANSKAFFELSE = NOT_SET_STRING,
		MAKS_ANT_PLASSER = NOT_SET_INT,
		MAKS_ANT_SOKERE = NOT_SET_INT,
		STATUS_FAST_ANT_PLASSER = NOT_SET_STRING,
		STATUS_SJEKK_ANT_DELTAKERE = NOT_SET_STRING,
		STATUS_KALKULATOR = NOT_SET_STRING,
		RAMMEAVTALE = NOT_SET_STRING,
		OPPLAERINGSGRUPPE = NOT_SET_STRING,
		HANDLINGSPLAN = NOT_SET_STRING,
		STATUS_SLUTTDATO = NOT_SET_STRING,
		MAKS_PERIODE = NOT_SET_INT,
		STATUS_MELDEPLIKT = NOT_SET_STRING,
		STATUS_VEDTAK = NOT_SET_STRING,
		STATUS_IA_AVTALE = NOT_SET_STRING,
		STATUS_TILLEGGSSTONADER = NOT_SET_STRING,
		STATUS_UTDANNING = NOT_SET_STRING,
		AUTOMATISK_TILSAGNSBREV = NOT_SET_STRING,
		STATUS_BEGRUNNELSE_INNSOKT = NOT_SET_STRING,
		STATUS_HENVISNING_BREV = NOT_SET_STRING,
		STATUS_KOPIBREV = NOT_SET_STRING,
	)
}

private fun emptyArenaTiltakDeltaker(): ArenaDeltaker {
	val NOT_SET_STRING = "NOT_SET_STRING"
	val NOT_SET_INT = Int.MIN_VALUE
	val NOT_SET_LONG = Long.MIN_VALUE
	val NOT_SET_FLOAT = Float.MIN_VALUE

	return ArenaDeltaker(
		TILTAKDELTAKER_ID = NOT_SET_LONG,
		PERSON_ID = NOT_SET_LONG,
		TILTAKGJENNOMFORING_ID = NOT_SET_LONG,
		DELTAKERSTATUSKODE = NOT_SET_STRING,
		DELTAKERTYPEKODE = NOT_SET_STRING,
		AARSAKVERDIKODE_STATUS = NOT_SET_STRING,
		OPPMOTETYPEKODE = NOT_SET_STRING,
		PRIORITET = NOT_SET_INT,
		BEGRUNNELSE_INNSOKT = NOT_SET_STRING,
		BEGRUNNELSE_PRIORITERING = NOT_SET_STRING,
		REG_DATO = NOT_SET_STRING,
		REG_USER = NOT_SET_STRING,
		MOD_DATO = NOT_SET_STRING,
		MOD_USER = NOT_SET_STRING,
		DATO_SVARFRIST = NOT_SET_STRING,
		DATO_FRA = NOT_SET_STRING,
		DATO_TIL = NOT_SET_STRING,
		BEGRUNNELSE_STATUS = NOT_SET_STRING,
		PROSENT_DELTID = NOT_SET_FLOAT,
		BRUKERID_STATUSENDRING = NOT_SET_STRING,
		DATO_STATUSENDRING = NOT_SET_STRING,
		AKTIVITET_ID = NOT_SET_LONG,
		BRUKERID_ENDRING_PRIORITERING = NOT_SET_STRING,
		DATO_ENDRING_PRIORITERING = NOT_SET_STRING,
		DOKUMENTKODE_SISTE_BREV = NOT_SET_STRING,
		STATUS_INNSOK_PAKKE = NOT_SET_STRING,
		STATUS_OPPTAK_PAKKE = NOT_SET_STRING,
		OPPLYSNINGER_INNSOK = NOT_SET_STRING,
		PARTISJON = NOT_SET_INT,
		BEGRUNNELSE_BESTILLING = NOT_SET_STRING,
		ANTALL_DAGER_PR_UKE = NOT_SET_INT
	)

}
