package no.nav.arena_tiltak_aktivitet_acl.services

import no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging.OppfolgingClient
import no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging.Oppfolgingsperiode
import no.nav.arena_tiltak_aktivitet_acl.utils.SecureLog.secureLog
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.chrono.ChronoZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAmount
import kotlin.math.abs

@Service
open class OppfolgingsperiodeService(
	private val oppfolgingClient: OppfolgingClient
) {

	companion object {
		fun tidspunktRettFoerStartDatoEllerSenere(tidspunkt: LocalDateTime, startDato: LocalDateTime, slakk: TemporalAmount): Boolean {
			return tidspunkt.plus(slakk).isAfter(startDato)
		}
		fun tidspunktTidligereEnnRettFoerStartDato(tidspunkt: LocalDateTime, startDato: LocalDateTime, slakk: TemporalAmount): Boolean {
			return !tidspunktRettFoerStartDatoEllerSenere(tidspunkt, startDato, slakk)
		}
		val defaultSlakk = Duration.of(7, ChronoUnit.DAYS)
	}


	fun finnOppfolgingsperiode(fnr: String, tidspunkt: LocalDateTime): Oppfolgingsperiode? {
		val oppfolgingsperioder = oppfolgingClient.hentOppfolgingsperioder(fnr)
			.sortedByDescending { it.startDato }
		if (oppfolgingsperioder.isEmpty()) {
			secureLog.info(
				"Arenatiltak finn oppfølgingsperiode - bruker har ingen oppfølgingsperioder - fnr={}, tidspunkt={}, oppfolgingsperioder={}",
				fnr, tidspunkt, listOf<Oppfolgingsperiode>()
			)
			return null
		}

		val tidspunktCZDT = ChronoZonedDateTime.from(tidspunkt.atZone(ZoneId.systemDefault()))
		val oppfolgingsperiode = oppfolgingsperioder
			.find {periode -> periode.tidspunktInnenforPeriode(tidspunktCZDT) }

		return oppfolgingsperiode ?: oppfolgingsperioder
				.filter { it.sluttDato == null || it.sluttDato.isAfter(tidspunktCZDT) }
				.minByOrNull { abs(ChronoUnit.MILLIS.between(tidspunktCZDT, it.startDato)) }
				.let { periodeMatch ->
					if (periodeMatch == null || !tidspunktRettFoerStartDatoEllerSenere(tidspunkt, periodeMatch.startDato.toLocalDateTime(), defaultSlakk)) {
						secureLog.info(
							"Arenatiltak finn oppfølgingsperiode - tidspunkt har ingen god match på oppfølgingsperioder) - fnr={}, tidspunkt={}, oppfolgingsperioder={}",
							fnr, tidspunkt, oppfolgingsperioder
						)
						null
					} else {
						secureLog.info(
							"Arenatiltak finn oppfølgingsperiode - tidspunkt innen {} oppfølging startdato) - fnr={}, tidspunkt={}, oppfolgingsperioder={}",
							defaultSlakk, fnr, tidspunkt, oppfolgingsperioder
						)
						periodeMatch
					}
				}
	}
}
