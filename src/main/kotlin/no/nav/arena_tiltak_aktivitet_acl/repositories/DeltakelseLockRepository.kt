package no.nav.arena_tiltak_aktivitet_acl.repositories

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.io.Closeable
import java.sql.ResultSet

@Component
class DeltakelseLockRepository(
	private val template: NamedParameterJdbcTemplate
) {
	private val log = LoggerFactory.getLogger(javaClass)
	private fun lockDeltakelse(deltakelseId: DeltakelseId) {
		@Language("postgresql")
		val sql = """
			SELECT pg_advisory_lock(:deltakelseId);
		""".trimIndent()
		template.query(sql, mapOf("deltakelseId" to deltakelseId.value)) {}
	}

	private fun unlockDeltakelse(deltakelseId: DeltakelseId) {
		@Language("postgresql")
		val sql = """
			SELECT pg_advisory_unlock(:deltakelseId);
		""".trimIndent()
		template.query(sql, mapOf("deltakelseId" to deltakelseId.value)) {}
	}

	fun safeDeltakelse(deltakelseId: DeltakelseId): SafeDeltakelse {
		lockDeltakelse(deltakelseId)
		log.info("Lock on $deltakelseId")
		return SafeDeltakelse(this, deltakelseId)
	}

	class SafeDeltakelse(val deltakelseLockRepository: DeltakelseLockRepository, val deltakelseId: DeltakelseId): Closeable {
		private val log = LoggerFactory.getLogger(javaClass)
		override fun close() {
			deltakelseLockRepository.unlockDeltakelse(deltakelseId)
			log.info("Unlock on $deltakelseId")
		}
	}
}


