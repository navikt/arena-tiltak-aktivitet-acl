package no.nav.arena_tiltak_aktivitet_acl.services

import no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging.OppfolgingClient
import no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging.Oppfolgingsperiode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
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
