package no.nav.arena_tiltak_aktivitet_acl.historiserteDeltakerFix

import io.getunleash.Unleash
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
import no.nav.arena_tiltak_aktivitet_acl.utils.asValidatedLocalDateTime
import no.nav.common.job.leader_election.LeaderElectionClient
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import kotlin.random.Random

/* Fikser deltakerlser som har blitt slettet fra tiltaksdeltaker tabellen
* Enten har vi gått glipp av slettemelding og står i feil state
* eller så mangler vi deltakelsen helt (feks ARENTAH fra synkrone endpunkt)
*   */
@Component
class DeletedMessagesFixSchedule(
	val historiskDeltakelseRepo: HistoriskDeltakelseRepo,
	val arenaDataRepository: ArenaDataRepository,
	val aktivitetskortIdRepository: AktivitetskortIdRepository,
	val leaderElectionClient: LeaderElectionClient,
	val unleash: Unleash
) {
	var minimumpos = 100493841434

	@Scheduled(fixedDelay = 10 * 1000L, initialDelay = ONE_MINUTE)
	fun prosesserDataFraHistoriskeDeltakelser() {
		if (!leaderElectionClient.isLeader && unleash.isEnabled("aktivitet-arena-acl.deletedMessagesFix.enabled")) return
		hentNesteBatchMedHistoriskeDeltakelser()
			.map { it.utledFixMetode() }
			.forEach { fix: FixMetode ->
				when (fix) {
					is Ignorer -> {}
					is OpprettMedLegacyId -> {
						// Bruk ID-som allerede eksisterer i Veilarbaktivitet
						aktivitetskortIdRepository.getOrCreate(fix.deltakelseId, AktivitetKategori.TILTAKSAKTIVITET, fix.funksjonellId)
						val nextPos = hentPosFraHullet()
//						arenaDataRepository.upsert(fix.toArenaDataUpsertInput(nextPos))
					}
					is Opprett -> {
//						arenaDataRepository.upsert(fix.toArenaDataUpsertInput(hentPosFraHullet()))
					}
					is Oppdater -> {
//						arenaDataRepository.upsert(fix.toArenaDataUpsertInput(hentPosFraHullet()))
					}
				}
				historiskDeltakelseRepo.oppdaterFixMetode(fix)
			}
	}

	fun hentPosFraHullet(): OperationPos {
		// TODO: Bruk posisjonene fra hullet
		/*
		operation_pos 100493841434 til 109986616390 er ledige,
		9492774956 ledige plasser

		select width_bucket(cast(operation_pos as numeric) , 2130012227006, 2800480067873, 10000000) as bucket,
       		count(*) as frequency from arena_data
                             where cast(operation_pos as numeric) > 2130012227006
                         group by bucket order by bucket
		;

		(2800480067873-2130012227006)/10000000 - bucket size 67046
		hull mellom bucket nr 1498878 og 1640466
		altså fra 1498879 til 1640465 (141586 buckets) -
 		altså fra pos 1498879 * 67046 til 1640465 * 67046
 		altså pos 100493841434 til 109986616390
 		9492774956 ledige plasser
		 */
		minimumpos++
		return OperationPos.of(minimumpos.toString())
	}

	fun hentNesteBatchMedHistoriskeDeltakelser(): List<HistoriskDeltakelse> {
		return  historiskDeltakelseRepo.getHistoriskeDeltakelser()
	}

	fun HistoriskDeltakelse.utledFixMetode(): FixMetode {
		val matcher =
			historiskDeltakelseRepo.finnEksisterendeDeltakelserForGjennomforing(person_id, tiltakgjennomforing_id)
				.filter { it.latestModDato == this.dato_statusendring?.asValidatedLocalDateTime("dato_statusendring") }
		return when {
			// Bare 1 kan matche
			matcher.size > 1 -> throw IllegalArgumentException("Flere matcher på historiske, ${matcher.joinToString { it.deltakelseId.toString() }}")
			// Har ikke sett meldingen før
			matcher.size == 0 -> {
				val legacyId = historiskDeltakelseRepo.getLegacyId(this.person_id, this.tiltakgjennomforing_id)
				when {
					legacyId != null -> OpprettMedLegacyId(legacyId.deltakerId, this, legacyId.funksjonellId, generertPos = hentPosFraHullet())
					else -> Opprett(genererDeltakelseId(), this, generertPos = hentPosFraHullet())
				}
			}
			else -> { // 1 match
				val match = matcher.first()
				val arenaDeltakelse = finnArenaDeltakelse(match.deltakelseId)
				return when (harRelevanteForskjeller(arenaDeltakelse, this)) {
					true -> Oppdater(match.deltakelseId, arenaDeltakelse, this, generertPos = hentPosFraHullet())
					false -> Ignorer(this.hist_tiltakdeltaker_id, match.deltakelseId)
				}
			}
		}
	}

	fun finnArenaDeltakelse(deltakelseId: DeltakelseId): ArenaDeltakelse {
		return (arenaDataRepository.getMostRecentDeltakelse(deltakelseId)
			?: throw IllegalArgumentException("Fant ikke deltakelse i arena-data: ${deltakelseId.value}"))
			.toArenaDeltakelse()
	}
	fun genererDeltakelseId(): DeltakelseId {
		return DeltakelseId(Random.nextLong(10, 10000))
		// historiskDeltakelseRepo.getNextFreeDeltakerId()
	}

	fun harRelevanteForskjeller(arenaDeltakelse: ArenaDeltakelse, historiskDeltakelse: HistoriskDeltakelse): Boolean {
		return arenaDeltakelse.DELTAKERSTATUSKODE != historiskDeltakelse.deltakerstatuskode
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
