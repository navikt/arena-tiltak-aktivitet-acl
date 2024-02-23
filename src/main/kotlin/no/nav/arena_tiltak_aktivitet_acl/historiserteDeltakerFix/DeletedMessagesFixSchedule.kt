package no.nav.arena_tiltak_aktivitet_acl.historiserteDeltakerFix

import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataUpsertInput
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.OperationPos
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import no.nav.arena_tiltak_aktivitet_acl.utils.ONE_MINUTE
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/* Fikser deltakerlser som har blitt slettet fra tiltaksdeltaker tabellen
* Enten har vi gått glipp av slettemelding og står i feil state
* eller så mangler vi deltakelsen helt (feks ARENTAH fra synkrone endpunkt)
*   */
@Component
class DeletedMessagesFixSchedule(
	val journalfoertArenaDeltakelseRepo: JournalfoertArenaDeltakelseRepo,
	val arenaDataRepository: ArenaDataRepository
) {

	@Scheduled(fixedDelay = 10 * 1000L, initialDelay = ONE_MINUTE)
	fun prosesserDataFraJNTabell() {
		hentNesteBatch()
			.filter { sjekkAtTrengerOppdatering(it) }
			.forEach { insertIntoAreanData(it) }
	}

	fun hentNesteBatch(): List<JournalfoertArenaDeltakelse> {
		return  journalfoertArenaDeltakelseRepo.get("lol")
	}

	fun sjekkAtTrengerOppdatering(journalfoertArenaDeltakelse: JournalfoertArenaDeltakelse): Boolean {
		// TODO: Finn gammel deltakelse og sjekk mot den
		return true
	}

	fun insertIntoAreanData(journalfoertArenaDeltakelse: JournalfoertArenaDeltakelse) {
		arenaDataRepository.upsert(
			ArenaDataUpsertInput(
				ArenaTableName.DELTAKER,
				arenaId = journalfoertArenaDeltakelse.TILTAKDELTAKER_ID.toString(),
				operation = Operation.MODIFIED, // TODO: Eller created hvis ikke finnes?
				operationPosition = OperationPos.of("0"), // TODO: Bruk hullet
				operationTimestamp = LocalDateTime.MAX,
				ingestStatus = IngestStatus.NEW,
				ingestedTimestamp = LocalDateTime.now(),
				before = journalfoertArenaDeltakelse.toString(), // TODO: Gjør om til skikkelig JSON
				after = null // TODO: Dette simulerer slettemelding, er det riktig?
			)
		)
	}
}
