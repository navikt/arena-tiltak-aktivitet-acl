package no.nav.arena_tiltak_aktivitet_acl.utils

import com.fasterxml.jackson.databind.ObjectMapper

object JsonUtils {

	val objectMapper: ObjectMapper = ObjectMapperFactory.get()

	fun <T> fromJson(jsonStr: String, clazz: Class<T>): T {
		return objectMapper.readValue(jsonStr, clazz)
	}

}
