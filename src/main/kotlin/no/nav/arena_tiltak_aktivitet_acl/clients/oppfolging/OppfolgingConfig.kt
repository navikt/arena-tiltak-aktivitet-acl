package no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging

import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.time.ZonedDateTime
import java.util.*

@Configuration
open class OppfolgingConfig {

	@Value("\${veilarboppfolging.url}")
	lateinit var url: String

	@Value("\${veilarboppfolging.scope}")
	lateinit var scope: String

	@Bean
	@Profile("default")
	open fun oppfolgingClient(machineToMachineTokenClient: MachineToMachineTokenClient): OppfolgingClient {
		return OppfolgingClientImpl(
			baseUrl = url,
			tokenProvider = { machineToMachineTokenClient.createMachineToMachineToken(scope) },
		)
	}

	@Bean
	@Profile("integration")
	open fun localOppfolgingClient(): OppfolgingClient {
		return object : OppfolgingClient {
			override fun hentOppfolgingsperioder(fnr: String): List<Oppfolgingsperiode> {
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
