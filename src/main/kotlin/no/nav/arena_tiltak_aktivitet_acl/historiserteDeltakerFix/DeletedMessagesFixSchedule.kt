package no.nav.arena_tiltak_aktivitet_acl.historiserteDeltakerFix

import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataUpsertInput
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.OperationPos
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.ArenaDeltakelse
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetskortIdRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import no.nav.arena_tiltak_aktivitet_acl.utils.ONE_MINUTE
import no.nav.arena_tiltak_aktivitet_acl.utils.ObjectMapper
import no.nav.arena_tiltak_aktivitet_acl.utils.asValidatedLocalDate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/* Fikser deltakerlser som har blitt slettet fra tiltaksdeltaker tabellen
* Enten har vi gått glipp av slettemelding og står i feil state
* eller så mangler vi deltakelsen helt (feks ARENTAH fra synkrone endpunkt)
*   */
@Component
class DeletedMessagesFixSchedule(
	val arenaDeltakelseLoggRepo: ArenaDeltakelseLoggRepo,
	val arenaDataRepository: ArenaDataRepository,
	val aktivitetskortIdRepository: AktivitetskortIdRepository
) {
	private val mapper = ObjectMapper.get()

	@Scheduled(fixedDelay = 10 * 1000L, initialDelay = ONE_MINUTE)
	fun prosesserDataFraJNTabell() {
		hentNesteBatchMedSlettemeldinger()
			.filter { sjekkAtTrengerOppdatering(it) }
			.forEach { insertIntoAreanData(it) }
	}

	fun hentNesteBatchMedSlettemeldinger(): List<ArenaDeltakelseLogg> {
		return  arenaDeltakelseLoggRepo.getSlettemeldinger("lol")
	}

	fun sjekkAtTrengerOppdatering(arenaDeltakelseLogg: ArenaDeltakelseLogg): Boolean {
		// TODO: Finn gammel deltakelse og sjekk mot den
		return when (arenaDeltakelseLogg.hentStatus()) {
			FixStatus.HAR_DELTAKELSE_RIKTIG_STATUS -> false
			FixStatus.HAR_DELTAKELSE_FEIL_STATUS -> true
			FixStatus.HAR_IKKE_DELTAKELSE_MEN_HAR_TRANSLATION -> true
			FixStatus.HAR_IKKE_DELTAKELSE_NOEN_PLASSER -> false
		}
	}

	fun ArenaDeltakelseLogg.hentStatus(): FixStatus {
		val sisteArenaOppdatering = arenaDataRepository.getMostRecentDeltakelse(this.TILTAKDELTAKER_ID.toString())
		return when {
			sisteArenaOppdatering == null -> {
				val legacyId = aktivitetskortIdRepository.getLegacyId(DeltakelseId(this.TILTAKDELTAKER_ID))
				when {
					legacyId != null -> FixStatus.HAR_IKKE_DELTAKELSE_MEN_HAR_TRANSLATION
					else -> FixStatus.HAR_IKKE_DELTAKELSE_NOEN_PLASSER
				}
			}
			sisteArenaOppdatering.operation == Operation.DELETED -> FixStatus.HAR_DELTAKELSE_RIKTIG_STATUS
			harRelevanteForskjeller(sisteArenaOppdatering.toArenaDeltakelse(), this) -> FixStatus.HAR_DELTAKELSE_FEIL_STATUS
			!harRelevanteForskjeller(sisteArenaOppdatering.toArenaDeltakelse(), this) -> FixStatus.HAR_DELTAKELSE_RIKTIG_STATUS
			else -> FixStatus.HAR_DELTAKELSE_RIKTIG_STATUS
		}
	}

	fun harRelevanteForskjeller(arenaDeltakelse: ArenaDeltakelse, logg: ArenaDeltakelseLogg): Boolean {
		return arenaDeltakelse.DELTAKERSTATUSKODE != logg.DELTAKERSTATUSKODE.name
			|| arenaDeltakelse.PROSENT_DELTID?.toInt() != logg.PROSENT_DELTID
			|| arenaDeltakelse.DATO_FRA?.asValidatedLocalDate("DATO_FRA") != logg.DATO_FRA
			|| arenaDeltakelse.DATO_TIL?.asValidatedLocalDate("DATO_TIL") != logg.DATO_TIL
	}

	fun insertIntoAreanData(arenaDeltakelseLogg: ArenaDeltakelseLogg) {
		arenaDataRepository.upsert(
			ArenaDataUpsertInput(
				ArenaTableName.DELTAKER,
				arenaId = arenaDeltakelseLogg.TILTAKDELTAKER_ID.toString(),
				operation = Operation.MODIFIED, // TODO: Eller created hvis ikke finnes?
				operationPosition = OperationPos.of("0"), // TODO: Bruk hullet
				operationTimestamp = LocalDateTime.MAX,
				ingestStatus = IngestStatus.NEW,
				ingestedTimestamp = LocalDateTime.now(),
				before = arenaDeltakelseLogg.toString(), // TODO: Gjør om til skikkelig JSON
				after = null // TODO: Dette simulerer slettemelding, er det riktig?
			)
		)
	}
}

enum class FixStatus {
	HAR_DELTAKELSE_RIKTIG_STATUS, // All good, ikke gjør noe
	HAR_DELTAKELSE_FEIL_STATUS, // Fix status, simuler en slettemelding

	HAR_IKKE_DELTAKELSE_MEN_HAR_TRANSLATION,
	// Lag nytt kort i riktig status, Trygt å lage så lenge det er i ny tabell?

	HAR_IKKE_DELTAKELSE_NOEN_PLASSER,
	// Lag nytt kort, risikerer duplikater hvis kort egentlig finnes i veilarbaktivitet men ikk i ACL
}

val mapper = ObjectMapper.get()
fun ArenaDataDbo.toArenaDeltakelse(): ArenaDeltakelse {
	return when (this.operation) {
		Operation.DELETED -> this.before // Skal egentlig ikke skje?
		else -> this.after
	}
		.let { mapper.readValue(it, ArenaDeltakelse::class.java) }
}
