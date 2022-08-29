package no.nav.arena_tiltak_aktivitet_acl.mocks

import ArenaOrdsProxyClient
import no.nav.arena_tiltak_aktivitet_acl.clients.ordsproxy.Arbeidsgiver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class OrdsClientMock {

	companion object {
		val fnrHandlers = mutableMapOf<Long, () -> String?>()
		val virksomhetsHandler = mutableMapOf<Long, () -> String>()
	}

	@Bean
	open fun ordsProxyClient(): ArenaOrdsProxyClient {

		return object : ArenaOrdsProxyClient {
			override fun hentFnr(arenaPersonId: Long): String? {
				if (fnrHandlers[arenaPersonId] != null) {
					return fnrHandlers[arenaPersonId]!!.invoke()
				}

				return "12345"

			}

			override fun hentArbeidsgiver(arenaArbeidsgiverId: Long): Arbeidsgiver? {
				throw NotImplementedError()
			}

			override fun hentVirksomhetsnummer(arenaArbeidsgiverId: Long): String {
				if (virksomhetsHandler[arenaArbeidsgiverId] != null) {
					return virksomhetsHandler[arenaArbeidsgiverId]!!.invoke()
				}

				return "12345"
			}

		}
	}


}
