package no.nav.arena_tiltak_aktivitet_acl.historiserteDeltakerFix

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.ArenaDeltakelse
import java.time.LocalDate
import java.time.LocalDateTime

/*
JN_OPERATION,
JN_ORACLE_USER,
JN_DATETIME,
JN_APPLN,
JN_SESSION,
JN_TIMESTAMP,
TILTAKDELTAKER_ID,
PERSON_ID,
TILTAKGJENNOMFORING_ID,
DELTAKERSTATUSKODE,
DELTAKERTYPEKODE,
BEGRUNNELSE_PRIORITERING,
REG_DATO,
REG_USER,
MOD_DATO,
MOD_USER,
DATO_FRA,
DATO_TIL,
PROSENT_DELTID,
BRUKERID_STATUSENDRING,
DATO_STATUSENDRING,
AKTIVITET_ID,
BRUKERID_ENDRING_PRIORITERING,
DATO_ENDRING_PRIORITERING

 */
enum class JnOperation {INS, UPD, DEL}
enum class DeltakerStatusKode { AKTUELL, INFOMOETE, JATAKK, TILBUD, VENTELISTE, FULLF, GJENN, GJENN_AVB, GJENN_AVL, IKKAKTUELL, AVSLAG, DELAVB, IKKEM, NEITAKK}
data class ArenaDeltakelseLogg(
	val JN_OPERATION: JnOperation,
	val JN_ORACLE_USER: String,
	val JN_DATETIME: LocalDateTime? = null, //10.01.23 16:19
	val JN_APPLN: String? = null,
	val JN_SESSION: String? = null,
	val JN_TIMESTAMP: LocalDateTime? = null,   // 10.01.2023 16.19.12,792457000
	val TILTAKDELTAKER_ID: Long,
	val PERSON_ID: Long,
	val TILTAKGJENNOMFORING_ID: Long,
	val DELTAKERSTATUSKODE: DeltakerStatusKode,
	val DELTAKERTYPEKODE: String? = null,
	val BEGRUNNELSE_PRIORITERING: String? = null,
	val REG_DATO: String,
	val REG_USER: String,
	val MOD_DATO: String,
	val MOD_USER: String,
	val DATO_FRA: String? = null,
	val DATO_TIL: String? = null,
	val PROSENT_DELTID: Float? = null,
	val BRUKERID_STATUSENDRING: String,
	val DATO_STATUSENDRING: String,
	val AKTIVITET_ID: Long,
	val BRUKERID_ENDRING_PRIORITERING: String? = null,
	val DATO_ENDRING_PRIORITERING: String? = null,
)

fun ArenaDeltakelseLogg.toArenaDeltakelse(): ArenaDeltakelse {
	return ArenaDeltakelse(
		TILTAKDELTAKER_ID = this.TILTAKDELTAKER_ID,
		TILTAKGJENNOMFORING_ID = this.TILTAKGJENNOMFORING_ID,
		DELTAKERSTATUSKODE = this.DELTAKERSTATUSKODE.name,
		DELTAKERTYPEKODE = this.DELTAKERTYPEKODE,
		BEGRUNNELSE_PRIORITERING = this.BEGRUNNELSE_PRIORITERING,
		REG_DATO = this.REG_DATO,
		REG_USER = this.REG_USER,
		MOD_DATO = this.MOD_DATO,
		MOD_USER = this.MOD_USER,
		DATO_FRA = this.DATO_FRA,
		DATO_TIL = this.DATO_TIL,
		PROSENT_DELTID = this.PROSENT_DELTID,
		BRUKERID_STATUSENDRING = this.BRUKERID_STATUSENDRING,
		DATO_STATUSENDRING = this.DATO_STATUSENDRING,
		AKTIVITET_ID = this.AKTIVITET_ID,
		BRUKERID_ENDRING_PRIORITERING = this.BRUKERID_ENDRING_PRIORITERING,
		DATO_ENDRING_PRIORITERING = this.DATO_ENDRING_PRIORITERING,
		PERSON_ID = null,
		AARSAKVERDIKODE_STATUS = null,
		OPPMOTETYPEKODE = null,
		PRIORITET = null,
		BEGRUNNELSE_INNSOKT = null,
		DATO_SVARFRIST = null,
		BEGRUNNELSE_STATUS = null,
		DOKUMENTKODE_SISTE_BREV = null,
		STATUS_INNSOK_PAKKE = null,
		STATUS_OPPTAK_PAKKE = null,
		OPPLYSNINGER_INNSOK = null,
		PARTISJON = null,
		BEGRUNNELSE_BESTILLING = null,
		ANTALL_DAGER_PR_UKE = null
	)
}