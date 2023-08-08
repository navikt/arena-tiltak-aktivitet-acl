package no.nav.arena_tiltak_aktivitet_acl.gruppetiltak.kafka.arena

import no.nav.arena_tiltak_aktivitet_acl.gruppetiltak.GruppeTiltak
import no.nav.arena_tiltak_aktivitet_acl.utils.asValidatedLocalDate
import no.nav.arena_tiltak_aktivitet_acl.utils.asValidatedLocalDateTime

@Suppress("kotlin:S117")
data class ArenaGruppeTiltakEndretDto(
	val VEILEDNINGDELTAKER_ID: Long? = null,
	val MOTEPLAN_ID: Long? = null,
	val AKTIVITET_ID: Long,
	val VEILEDNINGAKTIVITET_ID: Long? = null,
	val AKTIVITETID: String, // "GA" + AKTIVITET_ID
	val AKTIVITET_TYPE_KODE: String, // Kodeverk, f.eks "IGVAL", men ikke samme som tiltak,
	val AKTIVITETSNAVN: String,
	val MOTEPLAN_STARTDATO: String?, // dato-tid, eks "2023-05-23 00:00:00" , klokkeslett ikke relevant?
	val MOTEPLAN_SLUTTDATO: String?, // dato-tid, eks "2023-05-23 00:00:00"
	val PERSON_ID: Long?, // arena personid
	val PERSONIDENT: String, // fnr/dnr
	val HENDELSE_ID: Long?,
	val OPPRETTET_DATO: String, // dato-tid, eks "2023-05-05 10:38:45"
	val OPPRETTET_AV: String, // Arena-saksbehandlerident eks "MRN0106"
	val ENDRET_DATO: String?, // dato-tid, eks "2023-05-05 10:38:45"
	val ENDRET_AV: String? // Arena-saksbehandlerident eks "MRN0106"
) {
	fun mapGruppeTiltak(): GruppeTiltak {
		return GruppeTiltak(
			arenaAktivitetId = AKTIVITET_ID,
			aktivitetstype = AKTIVITET_TYPE_KODE,
			aktivitetsnavn = AKTIVITETSNAVN,
			beskrivelse = null,
			datoFra = MOTEPLAN_STARTDATO?.asValidatedLocalDate("MOTEPLAN_STARTDATO"),
			datoTil = MOTEPLAN_SLUTTDATO?.asValidatedLocalDate("MOTEPLAN_SLUTTDATO"),
			motePlan = null,
			personId = PERSON_ID,
			personIdent = PERSONIDENT,
			opprettetTid = OPPRETTET_DATO.asValidatedLocalDateTime("OPPRETTET_DATO"),
			opprettetAv = OPPRETTET_AV,
			endretTid = ENDRET_DATO?.asValidatedLocalDateTime("ENDRET_DATO"),
			endretAv = ENDRET_AV
		)
	}
}

val eksempelMelding = """
	{
  "table": "ARENA_GOLDENGATE.AKTIVITET_GRUPPE",
  "op_type": "I",
  "op_ts": "2023-06-22 10:46:59.776664",
  "current_ts": "2023-06-22 13:21:44.312014",
  "pos": "00000000000517925698",
  "after": {
    "AKTIVITET_ID": 139901938,
    "AKTIVITETID": "GA139901938",
    "AKTIVITET_TYPE_KODE": "IGVAL",
    "AKTIVITET_TYPE_NAVN": "informasjonsmøte ved NAV lokalt",
    "AKTIVITET_STATUS_KODE": "PLAN",
    "AKTIVITET_STATUS_NAVN": "Planlagt",
    "AKTIVITET_PERIODE_FOM": "2023-03-07 00:00:00",
    "AKTIVITET_PERIODE_TOM": "2023-03-07 00:00:00",
    "AKTIVITET_BESKRIVELSE": "02-03-2023 NOA/0403 : Påmeldt.\n\n",
    "VEILEDNINGDELTAKER_ID": 4040583,
    "VEILEDNINGAKTIVITET_ID": 469559,
    "ARRANGEMENT_TYPE_KODE": "IGVAL",
    "ARRANGEMENT_TYPE_NAVN": "informasjonsmøte ved NAV lokalt",
    "ARRANGEMENT_STATUS_KODE": "PLAN",
    "ARRANGEMENT_STATUS_NAVN": "Planlagt",
    "ARRANGEMENT_BESKRIVELSE": "Informasjonsmøte om Lager- og logistikk-kurs med praksis.\n",
    "FRIST_PAMELDING_DATO": "2023-03-07 00:00:00",
    "FRITT_OPPTAK": "J",
    "MAX_ANTALL": 40,
    "MOTEPLAN_ID": 392786,
    "MOTEPLAN_START_DATO": "2023-03-07 00:00:00",
    "MOTEPLAN_SLUTT_DATO": "2023-03-07 00:00:00",
    "MOTEPLAN_START_KL_SLETT": "09:00:00",
    "MOTEPLAN_SLUTT_KL_SLETT": "11:00:00",
    "MOTEPLAN_STED": "NAV Hamar Triangelgården, Inngang Grønnegata",
    "PERSON_ID": 4852846,
    "PERSONIDENT": "13106618350",
    "HENDELSE_ID": 309955,
    "OPPRETTET_DATO": "2023-06-20 15:23:49",
    "OPPRETTET_AV": "SKRIPT",
    "ENDRET_DATO": "2023-06-20 15:23:49",
    "ENDRET_AV": "SKRIPT"
  }
}
""".trimIndent()


// @SONAR_STOP@
