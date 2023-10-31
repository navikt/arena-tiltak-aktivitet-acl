package no.nav.arena_tiltak_aktivitet_acl.repositories

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import org.intellij.lang.annotations.Language
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.io.Closeable

@Component
class DeltakelseLockRepository(
	private val template: NamedParameterJdbcTemplate
) {
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
		return SafeDeltakelse(this, deltakelseId)
	}

	class SafeDeltakelse(val deltakelseLockRepository: DeltakelseLockRepository, val deltakelseId: DeltakelseId): Closeable {
		override fun close() {
			deltakelseLockRepository.unlockDeltakelse(deltakelseId)
		}
	}
}


