package no.nav.arena_tiltak_aktivitet_acl.processors.converters

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.amt.AmtGjennomforing
import org.springframework.dao.DataIntegrityViolationException

class GjennomforingStatusConverter {

	val avsluttendeStatuser = listOf("AVLYST", "AVBRUTT", "AVSLUTT")
	val ikkeStartetStatuser = listOf("PLANLAGT")
	val gjennomforesStatuser = listOf("GJENNOMFOR")

	fun convert (arenaStatus: String) : AmtGjennomforing.Status {
		return when (arenaStatus) {
			in avsluttendeStatuser -> AmtGjennomforing.Status.AVSLUTTET
			in ikkeStartetStatuser -> AmtGjennomforing.Status.IKKE_STARTET
			in gjennomforesStatuser -> AmtGjennomforing.Status.GJENNOMFORES
			else -> throw DataIntegrityViolationException("Ukjent status fra arena: $arenaStatus")
		}

	}
}
