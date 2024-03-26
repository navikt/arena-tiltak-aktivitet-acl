package no.nav.arena_tiltak_aktivitet_acl.historiserteDeltakerFix

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.ArenaDeltakelse
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import no.nav.arena_tiltak_aktivitet_acl.utils.asLocalDateTime

data class HistoriskDeltakelse(
	val hist_tiltakdeltaker_id: Long,
	val person_id: Long,
	val tiltakgjennomforing_id: Long,
	val deltakerstatuskode: String,
	val deltakertypekode: String?,
	val aarsakverdikode_status: String?,
	val oppmotetypekode: String?,
	val prioritet: String?,
	val prosent_deltid: String?,
	val brukerid_statusendring: String,
	val dato_statusendring: String?,
	val dato_svarfrist: String?,
	val dato_fra: String?,
	val dato_til: String?,
	val aktivitet_id: String?,
	val reg_dato: String,
	val reg_user: String,
	val mod_dato: String,
	val mod_user: String,
	val brukerid_endring_prioritering: String?,
	val dato_endring_prioritering: String?,
	val dokumentkode_siste_brev: String?,
	val rekkefolge: Int
)

data class SlettetDeltakelse (
	val data: HistoriskDeltakelse
)

data class TaptDeltakelse (
	val data: HistoriskDeltakelse,
	val operation: String
)

fun HistoriskDeltakelse.toArenaDeltakelse(deltakelseId: DeltakelseId): ArenaDeltakelse {
	return ArenaDeltakelse(
		TILTAKDELTAKER_ID = deltakelseId.value,
		TILTAKGJENNOMFORING_ID = this.tiltakgjennomforing_id,
		DELTAKERSTATUSKODE = this.deltakerstatuskode,
		DELTAKERTYPEKODE = this.deltakertypekode,
		BEGRUNNELSE_PRIORITERING = null, // sletter denne
		REG_DATO = this.reg_dato.asLocalDateTime().format(arenaYearfirstFormat),
		REG_USER = this.reg_user,
		MOD_DATO = this.mod_dato.asLocalDateTime().format(arenaYearfirstFormat),
		MOD_USER = this.mod_user,
		DATO_FRA = this.dato_fra?.asLocalDateTime()?.format(arenaYearfirstFormat),
		DATO_TIL = this.dato_til?.asLocalDateTime()?.format(arenaYearfirstFormat),
		PROSENT_DELTID = this.prosent_deltid?.toFloat(),
		BRUKERID_STATUSENDRING = this.brukerid_statusendring,
		DATO_STATUSENDRING = this.dato_statusendring?.asLocalDateTime()?.format(arenaYearfirstFormat),
		AKTIVITET_ID = this.aktivitet_id?.toLong(),
		BRUKERID_ENDRING_PRIORITERING = this.brukerid_endring_prioritering,
		DATO_ENDRING_PRIORITERING = this.dato_endring_prioritering,
		PERSON_ID = this.person_id,
		AARSAKVERDIKODE_STATUS = this.aarsakverdikode_status,
		OPPMOTETYPEKODE = this.oppmotetypekode,
		PRIORITET = this.prioritet?.toInt(),
		BEGRUNNELSE_INNSOKT = null,
		DATO_SVARFRIST = this.dato_svarfrist?.asLocalDateTime()?.format(arenaYearfirstFormat),
		BEGRUNNELSE_STATUS = null,
		DOKUMENTKODE_SISTE_BREV = this.dokumentkode_siste_brev,
		STATUS_INNSOK_PAKKE = null,
		STATUS_OPPTAK_PAKKE = null,
		OPPLYSNINGER_INNSOK = null,
		PARTISJON =  null,
		BEGRUNNELSE_BESTILLING = null,
		ANTALL_DAGER_PR_UKE = null
	)
}
