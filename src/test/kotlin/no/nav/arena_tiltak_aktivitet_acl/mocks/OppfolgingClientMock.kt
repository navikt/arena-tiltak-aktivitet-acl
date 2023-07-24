package no.nav.arena_tiltak_aktivitet_acl.mocks

import no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging.OppfolgingClient
import no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging.Oppfolgingsperiode
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.ZonedDateTime
import java.util.*

@Configuration
open class OppfolgingClientMock {

	companion object {
		val oppfolgingsperioder = mutableMapOf<String, List<Oppfolgingsperiode>>()
	}

	@Bean
	open fun oppfolgingClientClient(): OppfolgingClient {

		return object : OppfolgingClient {
			override fun hentOppfolgingsperioder(fnr: String): List<Oppfolgingsperiode> {
				if (oppfolgingsperioder[fnr] != null) {
					return oppfolgingsperioder[fnr]!!
				}
				return listOf(
					Oppfolgingsperiode(
						uuid = UUID.randomUUID(),
						startDato = ZonedDateTime.now().minusMonths(2),
						sluttDato = ZonedDateTime.now().minusMonths(1)
					),
					Oppfolgingsperiode(
						uuid = UUID.randomUUID(),
						startDato = ZonedDateTime.now().minusWeeks(2),
						sluttDato = null
					)
				)
			}
		}
	}


}
