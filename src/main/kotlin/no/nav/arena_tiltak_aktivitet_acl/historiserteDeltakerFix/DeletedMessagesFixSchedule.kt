package no.nav.arena_tiltak_aktivitet_acl.historiserteDeltakerFix

import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.OperationPos
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.ArenaDeltakelse
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetskortIdRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.utils.ONE_MINUTE
import no.nav.arena_tiltak_aktivitet_acl.utils.ObjectMapper
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/* Fikser deltakerlser som har blitt slettet fra tiltaksdeltaker tabellen
* Enten har vi gått glipp av slettemelding og står i feil state
* eller så mangler vi deltakelsen helt (feks ARENTAH fra synkrone endpunkt)
*   */
@Component
class DeletedMessagesFixSchedule(
	val historiskDeltakelseRepo: HistoriskDeltakelseRepo,
	val arenaDataRepository: ArenaDataRepository,
	val aktivitetskortIdRepository: AktivitetskortIdRepository
) {
	@Scheduled(fixedDelay = 10 * 1000L, initialDelay = ONE_MINUTE)
	fun prosesserDataFraJNTabell() {
		hentNesteBatchMedHistoriskeDeltakelser()
			.map { it.utledFixMetode() }
			.forEach { fix: FixMetode ->
				when (fix) {
					is Ignorer -> {}
					is OpprettMedLegacyId -> {
						// Bruk ID-som allerede eksisterer i Veilarbaktivitet
						aktivitetskortIdRepository.getOrCreate(fix.deltakelseId, AktivitetKategori.TILTAKSAKTIVITET, fix.funksjonellId)
						arenaDataRepository.upsert(fix.toArenaDataUpsertInput(hentPosFraHullet()))
					}
					is Opprett -> arenaDataRepository.upsert(fix.toArenaDataUpsertInput(hentPosFraHullet()))
					is Oppdater -> arenaDataRepository.upsert(fix.toArenaDataUpsertInput(hentPosFraHullet()))
				}
				historiskDeltakelseRepo.oppdaterFixMetode(fix)
			}
	}

	fun hentPosFraHullet(): OperationPos {
		// TODO: Bruk posisjonene fra hullet
		return OperationPos.of("0")
	}

	fun hentNesteBatchMedHistoriskeDeltakelser(): List<HistoriskDeltakelse> {
		return  historiskDeltakelseRepo.getHistoriskeDeltakelser("lol")
	}


	fun HistoriskDeltakelse.utledFixMetode(deltakelseId: DeltakelseId): FixMetode {
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

	fun harRelevanteForskjeller(arenaDeltakelse: ArenaDeltakelse, historiskDeltakelse: HistoriskDeltakelse): Boolean {
		return arenaDeltakelse.DELTAKERSTATUSKODE != historiskDeltakelse.deltakerstatuskode.
			|| arenaDeltakelse.PROSENT_DELTID != historiskDeltakelse.prosent_deltid?.toFloat()
			|| arenaDeltakelse.DATO_FRA  != historiskDeltakelse.dato_fra
			|| arenaDeltakelse.DATO_TIL  != historiskDeltakelse.dato_til
	}
}

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
