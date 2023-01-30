package no.nav.arena_tiltak_aktivitet_acl.services

import no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging.OppfolgingClient
import no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging.Oppfolgingsperiode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.chrono.ChronoZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Comparator.comparingLong
import kotlin.math.abs

@Service
open class OppfolgingsperiodeService(
	private val oppfolgingClient: OppfolgingClient
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun innenEnUke(opprettetTidspunkt: LocalDateTime, periodeStartDato: ZonedDateTime ): Boolean {
		return opprettetTidspunkt.plus(7, ChronoUnit.DAYS).isAfter(periodeStartDato.toLocalDateTime())
	}

	fun finnOppfolgingsperiode(fnr: String, opprettetTidspunkt: LocalDateTime): Oppfolgingsperiode? {
		val oppfolgingsperioder = oppfolgingClient.hentOppfolgingsperioder(fnr)

		if (oppfolgingsperioder.isEmpty()) {
			log.info(
				"Arenatiltak finn oppfølgingsperiode - bruker har ingen oppfølgingsperioder - fnr={}, opprettetTidspunkt={}, oppfolgingsperioder={}",
				fnr,
				opprettetTidspunkt,
				listOf<Oppfolgingsperiode>()
			)
			return null
		}

		val oppfolgingsperioderCopy = oppfolgingsperioder.toMutableList()
		oppfolgingsperioderCopy.sortWith(Comparator.comparing(Oppfolgingsperiode::startDato).reversed())

		val opprettetTidspunktCZDT = ChronoZonedDateTime.from(opprettetTidspunkt.atZone(ZoneId.systemDefault()))
		val maybePeriode = oppfolgingsperioderCopy
			.stream()
			.filter {
				((it.startDato.isBefore(opprettetTidspunktCZDT) || it.startDato.isEqual(opprettetTidspunktCZDT)) && it.sluttDato == null) ||
					((it.startDato.isBefore(opprettetTidspunktCZDT) || it.startDato.isEqual(opprettetTidspunktCZDT)) && it.sluttDato!!.isAfter(
						opprettetTidspunktCZDT
					))
			}
			.findFirst()

		return maybePeriode.orElseGet {
			oppfolgingsperioderCopy
				.stream()
				.filter { it.sluttDato == null || it.sluttDato.isAfter(opprettetTidspunktCZDT) }
				.min(comparingLong { abs(ChronoUnit.MILLIS.between(opprettetTidspunktCZDT, it.startDato)) })
				.filter {
					val innenEnUke = innenEnUke(opprettetTidspunkt, it.startDato)
					if (innenEnUke) {
						log.info(
							"Arenatiltak finn oppfølgingsperiode - opprettetdato innen 1 uke oppfølging startdato) - fnr={}, opprettetTidspunkt={}, oppfolgingsperioder={}",
							fnr,
							opprettetTidspunkt,
							oppfolgingsperioder
						)
					}
					innenEnUke
				}.orElseGet {
					log.info(
						"Arenatiltak finn oppfølgingsperiode - opprettetTidspunkt har ingen god match på oppfølgingsperioder) - fnr={}, opprettetTidspunkt={}, oppfolgingsperioder={}",
						fnr,
						opprettetTidspunkt,
						oppfolgingsperioder
					)
					null
				}
		}
	}
}
