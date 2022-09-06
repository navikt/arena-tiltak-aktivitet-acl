package no.nav.arena_tiltak_aktivitet_acl.clients.amt_enhetsregister

import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
open class AmtEnhetsregisterConfig {

	@Value("\${amt-enhetsregister.url}")
	lateinit var url: String

	@Value("\${amt-enhetsregister.scope}")
	lateinit var scope: String

	@Bean
	@Profile("default")
	open fun enhetsregisterClient(machineToMachineTokenClient: MachineToMachineTokenClient): EnhetsregisterClient {
		return AmtEnhetsregisterClient(
			baseUrl = url,
			tokenProvider = { machineToMachineTokenClient.createMachineToMachineToken(scope) },
		)
	}

	@Bean
	@Profile("integration")
	open fun localEnhetsregisterClient(): EnhetsregisterClient {
		return object : EnhetsregisterClient {
			override fun hentVirksomhet(organisasjonsnummer: String): Virksomhet {
				return Virksomhet(
					navn = "virksomhetnavn",
					organisasjonsnummer = "999",
					overordnetEnhetOrganisasjonsnummer = "888",
					overordnetEnhetNavn = "overordnet enhet AS"
				)
			}
		}
	}

}
