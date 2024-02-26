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
import java.util.*

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
	@Scheduled(fixedDelay = 10 * 1000L, initialDelay = ONE_MINUTE)
	fun prosesserDataFraJNTabell() {
		hentNesteBatchMedSlettemeldinger()
			.map { it.utledFixMetode() }
			.forEach {
				when (it) {
					is Ignorer -> {

					}
					is Opprett -> {

					}
					is OpprettMedLegacyId -> {

					}
					is Oppdater -> {

					}
				}
				arenaDeltakelseLoggRepo.oppdaterFixMetode(it)
			}
	}

	fun hentNesteBatchMedSlettemeldinger(): List<ArenaDeltakelseLogg> {
		return  arenaDeltakelseLoggRepo.getSlettemeldinger("lol")
	}


	fun ArenaDeltakelseLogg.utledFixMetode(): FixMetode {
		val deltakelseId = DeltakelseId(this.TILTAKDELTAKER_ID)
		val sisteArenaOppdatering = arenaDataRepository.getMostRecentDeltakelse(deltakelseId)
		return when {
			sisteArenaOppdatering == null -> {
				val legacyId = aktivitetskortIdRepository.getLegacyId(deltakelseId)
				when {
					legacyId != null -> OpprettMedLegacyId(deltakelseId, this, legacyId)
					else -> Opprett(deltakelseId, this)
				}
			}
			harRelevanteForskjeller(sisteArenaOppdatering.toArenaDeltakelse(), this) -> Oppdater(deltakelseId, sisteArenaOppdatering.toArenaDeltakelse(), this)
			else -> Ignorer(deltakelseId)
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

sealed class FixMetode (val deltakelseId: DeltakelseId) {
	}
class Ignorer(deltakelseId: DeltakelseId) : FixMetode(deltakelseId) {}
class Oppdater(deltakelseId: DeltakelseId, val arenaDeltakelse: ArenaDeltakelse, val arenaDeltakelseLogg: ArenaDeltakelseLogg): FixMetode(deltakelseId) {}
class OpprettMedLegacyId(deltakelseId: DeltakelseId, val arenaDeltakelseLogg: ArenaDeltakelseLogg, funksjonellId: UUID): FixMetode(deltakelseId) {}
class Opprett(deltakelseId: DeltakelseId, val arenaDeltakelseLogg: ArenaDeltakelseLogg): FixMetode(deltakelseId) {}

	/*
	HAR_DELTAKELSE_RIKTIG_STATUS, // All good, ikke gjør noe
	HAR_DELTAKELSE_FEIL_STATUS, // Fix status, simuler en slettemelding

	HAR_IKKE_DELTAKELSE_MEN_HAR_TRANSLATION,
	// Lag nytt kort i riktig status, Trygt å lage så lenge det er i ny tabell?

	HAR_IKKE_DELTAKELSE_NOEN_PLASSER,
	// Lag nytt kort, risikerer duplikater hvis kort egentlig finnes i veilarbaktivitet men ikk i ACL

	 */

val mapper = ObjectMapper.get()
fun ArenaDataDbo.toArenaDeltakelse(): ArenaDeltakelse {
	return when (this.operation) {
		Operation.DELETED -> this.before // Skal egentlig ikke skje?
		else -> this.after
	}
		.let { mapper.readValue(it, ArenaDeltakelse::class.java) }
}
