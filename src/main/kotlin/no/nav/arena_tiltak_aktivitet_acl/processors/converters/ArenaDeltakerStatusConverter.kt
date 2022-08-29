package no.nav.arena_tiltak_aktivitet_acl.processors.converters

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Aktivitet
import java.lang.IllegalArgumentException

/*
AKTUELL(PLANLAGT),
        INFOMOETE(PLANLAGT),
        JATAKK(PLANLAGT),
        TILBUD(PLANLAGT),
        VENTELISTE(PLANLAGT),
        FULLF(FULLFORT),
        GJENN(GJENNOMFORES),
        GJENN_AVB(AVBRUTT),
        GJENN_AVL(AVBRUTT),
        IKKAKTUELL(AVBRUTT),
        AVSLAG(AVBRUTT),
        DELAVB(AVBRUTT),
        IKKEM(AVBRUTT),
        NEITAKK(AVBRUTT);
 */
object ArenaDeltakerStatusConverter {
	fun toAktivitetStatus(status: String): Aktivitet.Status {
		return when (status) {
			"AKTUELL" -> Aktivitet.Status.PLANLAGT
			"INFOMOETE" -> Aktivitet.Status.PLANLAGT
			"JATAKK" -> Aktivitet.Status.PLANLAGT
			"TILBUD" -> Aktivitet.Status.PLANLAGT
			"VENTELISTE" -> Aktivitet.Status.PLANLAGT
			"FULLF" -> Aktivitet.Status.FULLFORT
			"GJENN" -> Aktivitet.Status.GJENNOMFORES
			"GJENN_AVB" -> Aktivitet.Status.AVBRUTT
			"GJENN_AVL" -> Aktivitet.Status.AVBRUTT
			"IKKAKTUELL" -> Aktivitet.Status.AVBRUTT
			"AVSLAG" -> Aktivitet.Status.AVBRUTT
			"DELAVB" -> Aktivitet.Status.AVBRUTT
			"IKKEM" -> Aktivitet.Status.AVBRUTT
			"NEITAKK" -> Aktivitet.Status.AVBRUTT
			else -> throw IllegalArgumentException("Ugyldig arenastatus $status kan ikke konverteres")
		}
	}
}
