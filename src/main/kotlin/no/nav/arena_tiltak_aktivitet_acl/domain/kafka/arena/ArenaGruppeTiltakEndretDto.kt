package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena

import no.nav.arena_tiltak_aktivitet_acl.utils.asValidatedLocalDate
import no.nav.arena_tiltak_aktivitet_acl.utils.asValidatedLocalDateTime

// @SONAR_START@
data class ArenaGruppeTiltakEndretDto(
	val VEILEDNINGDELTAKER_ID: Long? = null,
	val MOTEPLAN_ID: Long? = null,
	val AKTIVITET_ID: Long,
	val VEILEDNINGAKTIVITET_ID: Long? = null,
	val AKTIVITETID: String, // "GA" + AKTIVITET_ID
	val AKTIVITETSTYPE: String, // Kodeverk, f.eks "IGVAL", men ikke samme som tiltak,
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
			arenaAktivitetId = AKTIVITETID,
			aktivitetstype = AKTIVITETSTYPE,
			aktivitetsnavn = AKTIVITETSNAVN,
			beskrivelse = null,
			datoFra = MOTEPLAN_STARTDATO?.asValidatedLocalDate("MOTEPLAN_STARTDATO"),
			datoTil = MOTEPLAN_SLUTTDATO?.asValidatedLocalDate("MOTEPLAN_SLUTTDATO"),
			motePlan = null,
			personId = PERSON_ID,
			personIdent = PERSONIDENT,
			opprettetTid = OPPRETTET_DATO?.asValidatedLocalDateTime("OPPRETTET_DATO"),
			opprettetAv = OPPRETTET_AV,
			endretTid = ENDRET_DATO?.asValidatedLocalDateTime("ENDRET_DATO"),
			endretAv = ENDRET_AV
		)
	}
}

val eksempelMelding = """
	{
	  "table": "ARENA_GOLDENGATE.GRUPPEAKTIVITET",
	  "op_type": "I",
	  "op_ts": "2023-05-05 10:42:25.000000",
	  "current_ts": "2023-05-05 10:42:29.832000",
	  "pos": "00000001910249198628",
	  "after": {
	    "VEILEDNINGDELTAKER_ID": 4041988,
	    "MOTEPLAN_ID": 393087,
	    "AKTIVITET_ID": 138954103,
	    "VEILEDNINGAKTIVITET_ID": 469860,
	    "AKTIVITETID": "GA138954103",
	    "AKTIVITETSTYPE": "IGVAL",
	    "AKTIVITETSNAVN": "informasjonsmøte ved NAV lokalt",
	    "MOTEPLAN_STARTDATO": "2023-05-23 00:00:00",
	    "MOTEPLAN_SLUTTDATO": "2023-05-23 00:00:00",
	    "PERSON_ID": 277076,
	    "PERSONIDENT": "14047746095",
	    "HENDELSE_ID": 51663,
	    "OPPRETTET_DATO": "2023-05-05 10:38:45",
	    "OPPRETTET_AV": "MRN0106",
	    "ENDRET_DATO": "2023-05-05 10:38:45",
	    "ENDRET_AV": "MRN0106"
	  }
	}
""".trimIndent()


// @SONAR_STOP@
