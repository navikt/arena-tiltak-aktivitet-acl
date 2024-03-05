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
import no.nav.arena_tiltak_aktivitet_acl.utils.asBackwardsFormattedLocalDateTime
import no.nav.arena_tiltak_aktivitet_acl.utils.asValidatedLocalDateTime
import no.nav.common.job.JobRunner
import no.nav.common.job.leader_election.LeaderElectionClient
import org.slf4j.LoggerFactory
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
	val aktivitetskortIdRepository: AktivitetskortIdRepository,
	val leaderElectionClient: LeaderElectionClient,
	val unleash: Unleash
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@Scheduled(fixedDelay = 10 * 1000L, initialDelay = ONE_MINUTE)
	fun prosesserDataFraHistoriskeDeltakelser() {
		if (!leaderElectionClient.isLeader) return
		if (!unleash.isEnabled("aktivitet-arena-acl.deletedMessagesFix.enabled")) return
		JobRunner.run("prosesserDataFraHistoriskeDeltakelser") {
 		hentNesteBatchMedHistoriskeDeltakelser()
			.map {
				val fix = it.utledFixMetode()
				when (fix) {
					is Ignorer -> {}
					is OpprettMedLegacyId -> {
						log.info("OpprettMedLegacyId ${fix.deltakelseId}")
						// Bruk ID-som allerede eksisterer i Veilarbaktivitet
						aktivitetskortIdRepository.getOrCreate(fix.deltakelseId, AktivitetKategori.TILTAKSAKTIVITET, fix.funksjonellId)
						arenaDataRepository.upsertTemp(fix.toArenaDataUpsertInput())
					}
					is Opprett -> {
						log.info("Opprett ny for historisk deltakelseid ${fix.historiskDeltakelseId}")
						arenaDataRepository.upsertTemp(fix.toArenaDataUpsertInput())
					}
					is Oppdater -> {
						log.info("Oppdater eksisterende deltakerid ${fix.deltakelseId}")
						arenaDataRepository.upsertTemp(fix.toArenaDataUpsertInput())
					}
				}
				historiskDeltakelseRepo.oppdaterFixMetode(fix)
			}
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
		State.minimumpos++
		return OperationPos.of(State.minimumpos.toString())
	}

	fun hentNesteBatchMedHistoriskeDeltakelser(): List<HistoriskDeltakelse> {
		return  historiskDeltakelseRepo.getHistoriskeDeltakelser()
	}

	fun HistoriskDeltakelse.utledFixMetode(): FixMetode {
		val arenaDataDeltakelser =
			historiskDeltakelseRepo.finnEksisterendeDeltakelserForGjennomforing(person_id, tiltakgjennomforing_id) // alle deltakelser vi har i våre data for denne person-gjennomføring
		val matchMedFilter = arenaDataDeltakelser
			.filter { it.lastestStatusEndretDato == this.dato_statusendring?.asBackwardsFormattedLocalDateTime("dato_statusendring") } // er det noen av våre deltakelser som matcher med denne historisk deltakelsen?

		return when {
			arenaDataDeltakelser.isEmpty() -> throw IllegalStateException("SKal alltid ha deltakelse på person og gjennomforing!! person:${person_id} gjennomforing:${tiltakgjennomforing_id}")
			// Alt som kommer på relast er ikke slettet, hvis vi har bare 1, har den også kommet på relast
			arenaDataDeltakelser.size == 1 -> {
				log.info("Fant bare 1 eksisterende arenadeltakelse for historisk deltakelse ${this.hist_tiltakdeltaker_id}")
				val legacyId = historiskDeltakelseRepo.getLegacyId(this.person_id, this.tiltakgjennomforing_id)
				when {
					legacyId != null -> OpprettMedLegacyId(legacyId.deltakerId, this, legacyId.funksjonellId, generertPos = hentPosFraHullet())
					else -> Opprett(genererDeltakelseId(), this, generertPos = hentPosFraHullet())
				}
			}
			// Bare 1 kan matche
			matchMedFilter.size > 1 -> throw IllegalArgumentException("Flere matcher på historiske, ${matchMedFilter.joinToString { it.deltakelseId.toString() }}")
			// Har ikke sett meldingen før
			matchMedFilter.size == 0 -> {
				// Her kan det hende vi har den likevel, men dato_statusendring er ikke oppdatert hos oss. (hullet)
				log.info("Fant ingen eksisterende arenadeltakelse for historisk deltakelse ${this.hist_tiltakdeltaker_id}")
				val legacyId = historiskDeltakelseRepo.getLegacyId(this.person_id, this.tiltakgjennomforing_id) // Jovisst, vi hadde den likevel - OK
				// hvis legacy id finnes i arena_data -> Oppdater
				when {
					legacyId != null -> {
						if (historiskDeltakelseRepo.deltakelseExists(legacyId)) {  // Fant den den i translation, men vi har den i arena_data
							val arenaDeltakelse = finnArenaDeltakelse(legacyId.deltakerId, hentPosFraHullet())
							Oppdater(legacyId.deltakerId, arenaDeltakelse, this, generertPos = hentPosFraHullet())
						} else {
							OpprettMedLegacyId(legacyId.deltakerId, this, legacyId.funksjonellId, generertPos = hentPosFraHullet())
						}
					}
					else -> Opprett(genererDeltakelseId(), this, generertPos = hentPosFraHullet())
				}
			}
			else -> { // 1 match
				val match = matchMedFilter.first()
				val arenaDeltakelse = finnArenaDeltakelse(match.deltakelseId, OperationPos.of(match.latestOperationPos))
				return when (harRelevanteForskjeller(arenaDeltakelse, this)) {
					true -> Oppdater(match.deltakelseId, arenaDeltakelse, this, generertPos = hentPosFraHullet()) // denne treffer vi nok aldri. Hvis dato_statusendring er lik i matcher-filteret, så er dataene også like.
					false -> Ignorer(this.hist_tiltakdeltaker_id, match.deltakelseId)
				}
			}
		}
	}

	fun finnArenaDeltakelse(deltakelseId: DeltakelseId, operationPos: OperationPos): ArenaDeltakelse {
		return (historiskDeltakelseRepo.getMostRecentDeltakelse(deltakelseId, operationPos)
			?: throw IllegalArgumentException("Fant ikke deltakelse i arena-data: ${deltakelseId.value}"))
			.toArenaDeltakelse()
	}
	fun genererDeltakelseId(): DeltakelseId {
		return historiskDeltakelseRepo.getNextFreeDeltakerId(State.forrigeLedigeDeltakelse)
			.also { State.forrigeLedigeDeltakelse = it }
			.also { log.info("Fant ledig deltakelseId: ${it.value}") }
	}

	fun harRelevanteForskjeller(arenaDeltakelse: ArenaDeltakelse, historiskDeltakelse: HistoriskDeltakelse): Boolean {
		return arenaDeltakelse.DELTAKERSTATUSKODE != historiskDeltakelse.deltakerstatuskode
			|| arenaDeltakelse.PROSENT_DELTID != historiskDeltakelse.prosent_deltid?.toFloat()
			|| arenaDeltakelse.DATO_FRA?.asValidatedLocalDateTime("dato_fra")  != historiskDeltakelse.dato_fra?.asBackwardsFormattedLocalDateTime()
			|| arenaDeltakelse.DATO_TIL?.asValidatedLocalDateTime("dato_til")   != historiskDeltakelse.dato_til?.asBackwardsFormattedLocalDateTime()
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

object State {
	var minimumpos = 100493841434
	var forrigeLedigeDeltakelse = DeltakelseId(153)
}
