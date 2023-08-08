package no.nav.arena_tiltak_aktivitet_acl.services

import no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging.OppfolgingClient
import no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging.Oppfolgingsperiode
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Aktivitetskort
import no.nav.arena_tiltak_aktivitet_acl.exceptions.IgnoredException
import no.nav.arena_tiltak_aktivitet_acl.exceptions.OppfolgingsperiodeNotFoundException
import no.nav.arena_tiltak_aktivitet_acl.processors.DeltakerProcessor
import no.nav.arena_tiltak_aktivitet_acl.utils.SecureLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.chrono.ChronoZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

@Service
open class OppfolgingsperiodeService(
	private val oppfolgingClient: OppfolgingClient
) {
	private val log = LoggerFactory.getLogger(javaClass)

	companion object {
		fun mindreEnnEnUkeMellom(opprettetTidspunkt: LocalDateTime, periodeStartDato: LocalDateTime): Boolean {
			return opprettetTidspunkt.plusDays(7).isAfter(periodeStartDato)
		}
		fun merEnnEnUkeMellom(opprettetTidspunkt: LocalDateTime, periodeStartDato: LocalDateTime): Boolean {
			return !mindreEnnEnUkeMellom(opprettetTidspunkt, periodeStartDato)
		}
	}

	fun getOppfolgingsPeriodeOrThrow(aktivitet: Aktivitetskort, opprettetTidspunkt: LocalDateTime, tiltakDeltakerId: Long): DeltakerProcessor.AktivitetskortOppfolgingsperiode {
		val personIdent = aktivitet.personIdent
		val oppfolgingsperiode = finnOppfolgingsperiode(personIdent, opprettetTidspunkt)
			?.let { DeltakerProcessor.AktivitetskortOppfolgingsperiode(it.uuid, it.sluttDato) }
		if (oppfolgingsperiode == null) {
			SecureLog.secureLog.info("Fant ikke oppfølgingsperiode for personIdent=${personIdent}")
			val aktivitetStatus = aktivitet.aktivitetStatus
			val erFerdig = aktivitet.sluttDato?.isBefore(LocalDate.now()) ?: false
			when {
				aktivitetStatus.erAvsluttet() || erFerdig ->
					throw IgnoredException("Avsluttet deltakelse og ingen oppfølgingsperiode, id=${tiltakDeltakerId}")
				merEnnEnUkeMellom(opprettetTidspunkt, LocalDateTime.now()) ->
					throw IgnoredException("Opprettet for over 1 uke siden og ingen oppfølgingsperiode, id=${tiltakDeltakerId}")
				else -> throw OppfolgingsperiodeNotFoundException("Pågående deltakelse opprettetTidspunkt=${opprettetTidspunkt}, oppfølgingsperiode ikke startet/oppfolgingsperiode eldre enn en uke, id=${tiltakDeltakerId}")
			}
		} else {
			return oppfolgingsperiode
		}
	}


	fun finnOppfolgingsperiode(fnr: String, opprettetTidspunkt: LocalDateTime): Oppfolgingsperiode? {
		val oppfolgingsperioder = oppfolgingClient.hentOppfolgingsperioder(fnr)
			.sortedByDescending { it.startDato }
		if (oppfolgingsperioder.isEmpty()) {
			log.info(
				"Arenatiltak finn oppfølgingsperiode - bruker har ingen oppfølgingsperioder - fnr={}, opprettetTidspunkt={}, oppfolgingsperioder={}",
				fnr, opprettetTidspunkt, listOf<Oppfolgingsperiode>()
			)
			return null
		}

		val opprettetTidspunktCZDT = ChronoZonedDateTime.from(opprettetTidspunkt.atZone(ZoneId.systemDefault()))
		val oppfolgingsperiode = oppfolgingsperioder
			.find {periode -> periode.contains(opprettetTidspunktCZDT) }

		return oppfolgingsperiode ?: oppfolgingsperioder
				.filter { it.sluttDato == null || it.sluttDato.isAfter(opprettetTidspunktCZDT) }
				.minByOrNull { abs(ChronoUnit.MILLIS.between(opprettetTidspunktCZDT, it.startDato)) }
				.let { periodeMatch ->
					if (periodeMatch == null || !mindreEnnEnUkeMellom(opprettetTidspunkt, periodeMatch.startDato.toLocalDateTime())) {
						log.info(
							"Arenatiltak finn oppfølgingsperiode - opprettetTidspunkt har ingen god match på oppfølgingsperioder) - fnr={}, opprettetTidspunkt={}, oppfolgingsperioder={}",
							fnr, opprettetTidspunkt, oppfolgingsperioder
						)
						null
					} else {
						log.info(
							"Arenatiltak finn oppfølgingsperiode - opprettetdato innen 1 uke oppfølging startdato) - fnr={}, opprettetTidspunkt={}, oppfolgingsperioder={}",
							fnr, opprettetTidspunkt, oppfolgingsperioder
						)
						periodeMatch
					}
				}
	}
}
