package no.nav.arena_tiltak_aktivitet_acl.integration.commands.gruppetiltak

import com.fasterxml.jackson.databind.JsonNode
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaOperation
import no.nav.arena_tiltak_aktivitet_acl.gruppetiltak.GruppeTiltak
import no.nav.arena_tiltak_aktivitet_acl.gruppetiltak.kafka.arena.ArenaGruppeTiltakEndretDto
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.Command
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import no.nav.arena_tiltak_aktivitet_acl.utils.toArenaFormat
import java.time.LocalDateTime
import kotlin.random.Random

class GruppeTiltakCommand(val gruppeTiltak: GruppeTiltak) : Command(gruppeTiltak.arenaAktivitetId.toString()) {
	fun toArenaKafkaMessageDto(pos: String): ArenaKafkaMessageDto = ArenaKafkaMessageDto(
		table = ArenaTableName.GRUPPETILTAK,
		opType = ArenaOperation.I.name,
		opTs = LocalDateTime.now().format(opTsFormatter),
		pos = pos,
		before = null,
		after = createPayload(gruppeTiltak)
	)

	fun createPayload(input: GruppeTiltak): JsonNode {
		val aktivitetId = input.arenaAktivitetId
		val mote = input.motePlan.firstOrNull()
		val data = ArenaGruppeTiltakEndretDto(
			VEILEDNINGDELTAKER_ID = null,
			AKTIVITET_ID = aktivitetId,
			VEILEDNINGAKTIVITET_ID = null,
			AKTIVITETID = "GA$aktivitetId",// "GA" + AKTIVITET_ID
			AKTIVITET_TYPE_KODE = input.aktivitetstype,// Kodeverk, f.eks "IGVAL", men ikke samme som tiltak,
			AKTIVITETSNAVN = input.tittel,
			PERSON_ID = null, // arena personid
			PERSONIDENT = input.personIdent,// fnr/dnr
			HENDELSE_ID = Random.nextLong(),
			OPPRETTET_DATO = input.opprettetTid.toArenaFormat(),// dato-tid, eks "2023-05-05 10:38:45"
			OPPRETTET_AV = input.opprettetAv,// Arena-saksbehandlerident eks "MRN0106"
			ENDRET_DATO = input.endretTid?.toArenaFormat(), // dato-tid, eks "2023-05-05 10:38:45"
			ENDRET_AV = input.endretAv, // Arena-saksbehandlerident eks "MRN0106"
			AKTIVITET_PERIODE_FOM = input.datoFra.toArenaFormat(),
			AKTIVITET_PERIODE_TOM = input.datoTil.toArenaFormat(),
			ARRANGEMENT_BESKRIVELSE = input.beskrivelse,
			ARRANGEMENT_TYPE_NAVN = input.aktivitetstype,
			// Kan være flere møter men litt rar representasjon i kafkameldingene 🤷
			MOTEPLAN_ID = mote?.moteId,
			MOTEPLAN_START_DATO = mote?.fra?.toLocalDate()?.toArenaFormat(), // dato-tid, eks "2023-05-23 00:00:00" , klokkeslett ikke relevant?
			MOTEPLAN_START_KL_SLETT = mote?.fra?.toLocalTime()?.toArenaFormat(), // dato-tid, eks "2023-05-23 00:00:00" , klokkeslett ikke relevant?
			MOTEPLAN_SLUTT_DATO = mote?.til?.toLocalDate()?.toArenaFormat(), // dato-tid, eks "2023-05-23 00:00:00"
			MOTEPLAN_SLUTT_KL_SLETT = mote?.til?.toLocalTime()?.toArenaFormat(), // dato-tid, eks "2023-05-23 00:00:00"
			MOTEPLAN_STED = mote?.sted,
		)
		return objectMapper.readTree(objectMapper.writeValueAsString(data))
	}
}

