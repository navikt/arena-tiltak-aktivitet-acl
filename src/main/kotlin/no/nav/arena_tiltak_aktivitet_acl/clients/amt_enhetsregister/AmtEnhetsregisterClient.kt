package no.nav.arena_tiltak_aktivitet_acl.clients.amt_enhetsregister
import no.nav.common.json.JsonUtils
import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.function.Supplier

class AmtEnhetsregisterClient(
    private val baseUrl: String,
    private val tokenProvider: Supplier<String>,
    private val httpClient: OkHttpClient = baseClient(),
) : EnhetsregisterClient {

	override fun hentVirksomhet(organisasjonsnummer: String): Virksomhet? {
		val request = Request.Builder()
			.url("$baseUrl/api/enhet/$organisasjonsnummer")
			.addHeader("Authorization", "Bearer ${tokenProvider.get()}")
			.get()
			.build()

		httpClient.newCall(request).execute().use { response ->
			if (response.code == 404) {
				return null
			}

			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente enhet fra amt-enhetsregister. organisasjonsnummer=${organisasjonsnummer} status=${response.code}")
			}

			val body = response.body?.string() ?: throw RuntimeException("Body is missing")
			val enhetDto = JsonUtils.fromJson(body, EnhetDto::class.java)

			return Virksomhet(
				navn = enhetDto.navn,
				organisasjonsnummer = enhetDto.organisasjonsnummer,
				overordnetEnhetOrganisasjonsnummer = enhetDto.overordnetEnhetOrganisasjonsnummer,
				overordnetEnhetNavn = enhetDto.overordnetEnhetNavn,
			)
		}
	}

}
