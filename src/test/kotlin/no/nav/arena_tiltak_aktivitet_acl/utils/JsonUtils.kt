package no.nav.arena_tiltak_aktivitet_acl.utils

object JsonUtils {

	val objectMapper: com.fasterxml.jackson.databind.ObjectMapper = ObjectMapper.get()

	fun <T> fromJson(jsonStr: String, clazz: Class<T>): T {
		return objectMapper.readValue(jsonStr, clazz)
	}

}
