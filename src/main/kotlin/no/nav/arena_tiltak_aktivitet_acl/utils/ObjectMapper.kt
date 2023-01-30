package no.nav.arena_tiltak_aktivitet_acl.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object ObjectMapper {

	private val instance = jacksonObjectMapper()
		.registerKotlinModule()
		.registerModule(JavaTimeModule())
		.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
		.configure(MapperFeature.USE_STD_BEAN_NAMING, true)
		.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)

	fun get() = instance!!

}
