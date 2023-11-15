package no.nav.arena_tiltak_aktivitet_acl.services

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaKafkaMessage
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.OperationPos
import no.nav.arena_tiltak_aktivitet_acl.exceptions.IgnoredException
import no.nav.arena_tiltak_aktivitet_acl.processors.DeltakerProcessor
import no.nav.arena_tiltak_aktivitet_acl.processors.GjennomforingProcessor
import no.nav.arena_tiltak_aktivitet_acl.processors.TiltakProcessor
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import no.nav.arena_tiltak_aktivitet_acl.utils.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
open class RetryArenaMessageProcessorService(
	private val arenaDataRepository: ArenaDataRepository,
	private val tiltakProcessor: TiltakProcessor,
	private val gjennomforingProcessor: GjennomforingProcessor,
	private val deltakerProcessor: DeltakerProcessor,
	private val meterRegistry: MeterRegistry
) {

	private val log = LoggerFactory.getLogger(javaClass)

	private val mapper = ObjectMapper.get()

	companion object {
		private const val MAX_INGEST_ATTEMPTS = 10
	}

	fun processMessages(batchSize: Int = 2000) {
		processMessagesWithStatus(IngestStatus.RETRY, batchSize)
	}

	fun processFailedMessages(batchSize: Int = 500) {
		processMessagesWithStatus(IngestStatus.FAILED, batchSize)
	}

	private fun processMessagesWithStatus(status: IngestStatus, batchSize: Int) {
		processMessages(ArenaTableName.TILTAK, status, batchSize)
		processMessages(ArenaTableName.GJENNOMFORING, status, batchSize)
		processMessages(ArenaTableName.DELTAKER, status, batchSize)
	}

	private fun processMessages(tableName: ArenaTableName, status: IngestStatus, batchSize: Int) {
		var fromPos = OperationPos.of("0")
		var data: List<ArenaDataDbo>

		val start = Instant.now()
		var totalHandled = 0

		do {
			data = arenaDataRepository.getByIngestStatus(tableName, status, fromPos, batchSize)
			data.forEach { process(it) }
			totalHandled += data.size
			fromPos = data.maxByOrNull { it.operationPosition.value }?.operationPosition ?: break
		} while (data.isNotEmpty())

		// Sette QUEUED med lavest ID til RETRY
		val messagesMovedToRetry = arenaDataRepository.moveQueueForward()
		log.info("Moved $messagesMovedToRetry messages from QUEUED to RETRY")

		val duration = Duration.between(start, Instant.now())

		if (totalHandled > 0)
			log.info("[$tableName]: Handled $totalHandled $status messages in ${duration.toSeconds()}.${duration.toMillisPart()} seconds.")
	}


	private fun process(arenaDataDbo: ArenaDataDbo) {
		withTimer(arenaDataDbo.arenaTableName.tableName) {
			try {
				when (arenaDataDbo.arenaTableName) {
					ArenaTableName.TILTAK -> tiltakProcessor.handleArenaMessage(toArenaKafkaMessage(arenaDataDbo))
					ArenaTableName.GJENNOMFORING -> gjennomforingProcessor.handleArenaMessage(
						toArenaKafkaMessage(
							arenaDataDbo
						)
					)
					ArenaTableName.DELTAKER -> deltakerProcessor.handleArenaMessage(toArenaKafkaMessage(arenaDataDbo))
				}
			} catch (e: Throwable) {
				val currentIngestAttempts = arenaDataDbo.ingestAttempts + 1
				val hasReachedMaxRetries = currentIngestAttempts >= MAX_INGEST_ATTEMPTS

				if (e is IgnoredException) {
					log.info("${arenaDataDbo.id} in table ${arenaDataDbo.arenaTableName}: '${e.message}'")
					arenaDataRepository.updateIngestStatus(arenaDataDbo.id, IngestStatus.IGNORED)
				} else if (arenaDataDbo.ingestStatus == IngestStatus.RETRY && hasReachedMaxRetries) {
					arenaDataRepository.updateIngestStatus(arenaDataDbo.id, IngestStatus.FAILED)
				}
				log.error("feilet retry-behandling av medling med arenaId: ${arenaDataDbo.arenaId}, id: ${arenaDataDbo.id}, tabell: ${arenaDataDbo.arenaTableName}", e)
				arenaDataRepository.updateIngestAttempts(arenaDataDbo.id, currentIngestAttempts, e.message)
			}
		}
	}

	private inline fun <reified D> toArenaKafkaMessage(arenaDataDbo: ArenaDataDbo): ArenaKafkaMessage<D> {
		return ArenaKafkaMessage(
			arenaTableName = arenaDataDbo.arenaTableName,
			operationType = arenaDataDbo.operation,
			operationTimestamp = arenaDataDbo.operationTimestamp,
			operationPosition = arenaDataDbo.operationPosition,
			before = arenaDataDbo.before?.let { mapper.readValue(it, D::class.java) },
			after = arenaDataDbo.after?.let { mapper.readValue(it, D::class.java) }
		)
	}

	private fun withTimer(tableName: String, runnable: () -> Unit) {
		val timer = Timer.builder("batchProcessor")
			.publishPercentiles(0.5, 0.95) // median and 95th percentile
			.publishPercentileHistogram()
			.tag("tableName", tableName)
			.register(meterRegistry)

		timer.record(runnable)
	}

}
