package no.nav.arena_tiltak_aktivitet_acl.repositories

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.io.Closeable

@Component
open class AdvisoryLockRepository(
	private val template: NamedParameterJdbcTemplate
) {
	private val log = LoggerFactory.getLogger(javaClass)

	/**
	 * Acquire a transactional advisory lock on a lockId. The duration of the lock is that of the containing transaction,
	 * whence the methode requires a transaction already to be present before the call.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	open fun aquireTransactionalAdvisoryLock(lockId: Long) {
		@Language("postgresql")
		val sql = """
			SELECT pg_advisory_xact_lock(:lockId);
		""".trimIndent()
		template.query(sql, mapOf("lockId" to lockId)) {}
	}

	@Transactional(propagation = Propagation.MANDATORY)
	open fun lockDeltakelse(deltakelseId: DeltakelseId) {
		aquireTransactionalAdvisoryLock(deltakelseId.value)
	}

	@Transactional(propagation = Propagation.MANDATORY)
	open fun safeDeltakelse(deltakelseId: DeltakelseId): SafeDeltakelse {
		lockDeltakelse(deltakelseId)
		log.info("Acquired lock on deltakelseId: $deltakelseId")
		return SafeDeltakelse(deltakelseId)
	}

	class SafeDeltakelse(val deltakelseId: DeltakelseId): Closeable {
		private val log = LoggerFactory.getLogger(javaClass)
		override fun close() {
			log.info("Safe to release lock on deltakelseId: $deltakelseId")
		}
	}
}


