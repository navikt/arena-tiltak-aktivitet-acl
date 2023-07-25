package no.nav.arena_tiltak_aktivitet_acl.integration.utils

import no.nav.arena_tiltak_aktivitet_acl.integration.utils.Retry.nullableAsyncRetryHandler
import org.slf4j.LoggerFactory

fun <T> asyncRetryHandler(
	message: String,
	maxAttempts: Int = 20,
	sleepTime: Long = 250,
	executor: () -> T?,
): T {
	return nullableAsyncRetryHandler(message, maxAttempts, sleepTime, executor)
		?: throw IllegalStateException("Did not find data in $maxAttempts attempts.")
}

object Retry {
	private val log = LoggerFactory.getLogger(javaClass)
	fun <T> nullableAsyncRetryHandler(
		retryMessage: String,
		maxAttempts: Int = 20,
		sleepTime: Long = 250,
		executor: () -> T?,
	): T? {
		var attempts = 0
		while (attempts < maxAttempts) {
			val data = executor()

			if (data != null) {
				log.info("Stopped retrying!")
				return data
			}
			log.info("Retrying - ${retryMessage}")

			Thread.sleep(sleepTime)
			attempts++
		}
		return null
	}
}

