package no.nav.arena_tiltak_aktivitet_acl.clients.enhetsregister

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import no.nav.arena_tiltak_aktivitet_acl.clients.amt_enhetsregister.AmtEnhetsregisterClient
import no.nav.arena_tiltak_aktivitet_acl.clients.amt_enhetsregister.Virksomhet
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

@WireMockTest
class AmtEnhetsregisterClientTest {
	var proxyToken = "PROXY_TOKEN"

	@Test
	fun `should work` (wmRuntimeInfo: WireMockRuntimeInfo) {
		val client = AmtEnhetsregisterClient(
			baseUrl = wmRuntimeInfo.httpBaseUrl,
			tokenProvider = { proxyToken },
		)

		val orgNummer = "123123123"

		@Language("JSON")
		val body = """
			{
				"organisasjonsnummer": $orgNummer,
				"overordnetEnhetOrganisasjonsnummer": "123123",
				"overordnetEnhetNavn": "Test org",
				"navn": "Sigurd Kaker"
			}
		""".trimIndent()

		WireMock.givenThat(
			WireMock.get(WireMock.urlEqualTo("/api/enhet/$orgNummer"))
				.withHeader("Authorization", WireMock.equalTo("Bearer $proxyToken"))
				.willReturn(
					WireMock.aResponse()
						.withStatus(200)
						.withBody(body)
				)

		)

		Assertions.assertEquals(Virksomhet(
			organisasjonsnummer = orgNummer,
			overordnetEnhetOrganisasjonsnummer = "123123",
			overordnetEnhetNavn = "Test org",
			navn = "Sigurd Kaker"
		), client.hentVirksomhet(orgNummer))
	}

}
