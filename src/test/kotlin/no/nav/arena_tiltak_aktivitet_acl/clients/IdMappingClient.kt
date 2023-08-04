package no.nav.arena_tiltak_aktivitet_acl.clients

import kafka.utils.Json
import no.nav.arena_tiltak_aktivitet_acl.domain.dto.TranslationQuery
import no.nav.arena_tiltak_aktivitet_acl.utils.JsonUtils
import no.nav.common.rest.client.RestClient
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.http.HttpStatus
import java.util.function.Supplier

class IdMappingClient(
	private val port: Int,
	private val tokenProvider: Supplier<String>,
) {
	private val httpClient: OkHttpClient = RestClient.baseClient()
	private val baseUrl: String = "http://localhost:${port}"

	val JSON: MediaType = "application/json; charset=utf-8".toMediaType()

	private val objectMapper = JsonUtils.objectMapper
	fun hentMapping(translationDbo: TranslationQuery): String {
		val body: RequestBody = Json.encodeAsString(translationDbo).toRequestBody(JSON)
		val request = Request.Builder()
			.url("$baseUrl/api/translation/arenaid")
			.header("Authorization", "Bearer ${tokenProvider.get()}")
			.post(body)
			.build()
		httpClient.newCall(request).execute().use { response ->
			if (response.code == HttpStatus.NOT_FOUND.value()) throw RuntimeException("Fant ikke")
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente mapping for arenaId. Status: ${response.code}")
			}
			return response.body?.string() ?: throw RuntimeException("Body is missing")
		}
	}
}
