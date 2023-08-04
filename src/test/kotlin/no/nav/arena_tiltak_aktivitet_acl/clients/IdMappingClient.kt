package no.nav.arena_tiltak_aktivitet_acl.clients

import kafka.utils.Json
import no.nav.arena_tiltak_aktivitet_acl.domain.dto.TranslationQuery
import no.nav.arena_tiltak_aktivitet_acl.utils.JsonUtils
import no.nav.common.rest.client.RestClient
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.function.Supplier

class IdMappingClient(
	private val port: Int,
	private val tokenProvider: Supplier<String>,
) {
	private val httpClient: OkHttpClient = RestClient.baseClient()
	private val baseUrl: String = "http://localhost:${port}"

	val JSON: MediaType = "application/json; charset=utf-8".toMediaType()

	private val objectMapper = JsonUtils.objectMapper
	fun hentMapping(translationDbo: TranslationQuery): Pair<Response, UUID?> {
		val body: RequestBody = Json.encodeAsString(translationDbo).toRequestBody(JSON)
		val request = Request.Builder()
			.url("$baseUrl/api/translation/arenaid")
			.header("Authorization", "Bearer ${tokenProvider.get()}")
			.post(body)
			.build()
		httpClient.newCall(request).execute().use { response ->
			return response to
				if (response.isSuccessful) objectMapper.readValue(response.body?.string(), UUID::class.java) else null
		}
	}
}
