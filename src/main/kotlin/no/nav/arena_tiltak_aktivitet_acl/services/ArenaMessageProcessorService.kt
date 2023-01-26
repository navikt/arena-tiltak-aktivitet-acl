package no.nav.arena_tiltak_aktivitet_acl.services

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.db.toUpsertInput
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation.Companion.fromArenaOperationString
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaKafkaMessage
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.exceptions.*
import no.nav.arena_tiltak_aktivitet_acl.processors.*
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.utils.*
import no.nav.arena_tiltak_aktivitet_acl.utils.DateUtils.parseArenaDateTime
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
open class ArenaMessageProcessorService(
	private val tiltakProcessor: TiltakProcessor,
	private val gjennomforingProcessor: GjennomforingProcessor,
	private val deltakerProcessor: DeltakerProcessor,
	private val arenaDataRepository: ArenaDataRepository,
	private val meterRegistry: MeterRegistry
) {

	private val log = LoggerFactory.getLogger(javaClass)

	private val mapper = ObjectMapper.get()

	fun handleArenaGoldenGateRecord(record: ConsumerRecord<String, String>) {
		val recordValue = record.value().removeNullCharacters()
		val messageDto = mapper.readValue(recordValue, ArenaKafkaMessageDto::class.java)
		val messageAlreadyInStore = arenaDataRepository.exists(messageDto!!.table, fromArenaOperationString(messageDto.opType), messageDto.pos)
		if (messageAlreadyInStore) {
			log.warn("Ignorerer melding topic:${record.topic()} partition:${record.partition()} offset:${record.offset()} allerede lagret under table:${messageDto.table} optype:${messageDto.opType} pos:${messageDto.pos}")
			return
		}
		processArenaKafkaMessage(messageDto)
	}

	private fun processArenaKafkaMessage(messageDto: ArenaKafkaMessageDto) {
		val processorName = findProcessorName(messageDto.table)

		withTimer(processorName) {
			when (messageDto.table) {
				ARENA_TILTAK_TABLE_NAME -> process(messageDto, tiltakProcessor) { it.TILTAKSKODE }
				ARENA_GJENNOMFORING_TABLE_NAME -> process(messageDto, gjennomforingProcessor) { it.TILTAKGJENNOMFORING_ID.toString() }
				ARENA_DELTAKER_TABLE_NAME -> process(messageDto, deltakerProcessor) { it.TILTAKDELTAKER_ID.toString() }
				else -> throw IllegalArgumentException("Kan ikke håndtere melding fra ukjent arena tabell: ${messageDto.table}")
			}
		}
	}

	private inline fun <reified D> process(
		messageDto: ArenaKafkaMessageDto,
		processor: ArenaMessageProcessor<ArenaKafkaMessage<D>>,
		arenaIdExtractor: (msg: D) -> String
	) {
		val msg = toArenaKafkaMessage<D>(messageDto)
		val arenaId = arenaIdExtractor(msg.getData())
		val arenaTableName = msg.arenaTableName

		try {
			processor.handleArenaMessage(msg)
		} catch (e: Exception) {
			when (e) {
				is DependencyNotIngestedException -> {
					log.info("Dependency for $arenaId in table $arenaTableName is not ingested: '${e.message}'")
					arenaDataRepository.upsert(msg.toUpsertInput(arenaId, ingestStatus = IngestStatus.RETRY, note = e.message))
				}
				is OutOfOrderException -> {
					arenaDataRepository.upsert(msg.toUpsertInput(arenaId, ingestStatus = IngestStatus.QUEUED, note = e.message))
				}
				is ValidationException -> {
					log.info("$arenaId in table $arenaTableName is not valid: '${e.message}'")
					arenaDataRepository.upsert(msg.toUpsertInput(arenaId, ingestStatus = IngestStatus.INVALID, note = e.message))
				}
				is IgnoredException -> {
					log.info("$arenaId in table $arenaTableName: '${e.message}'")
					arenaDataRepository.upsert(msg.toUpsertInput(arenaId, ingestStatus = IngestStatus.IGNORED, note = e.message))
				}
				is OperationNotImplementedException -> {
					log.info("Operation not supported for $arenaId in table $arenaTableName: '${e.message}'")
					arenaDataRepository.upsert(msg.toUpsertInput(arenaId, ingestStatus = IngestStatus.FAILED, note = e.message))
				}
				is OppfolgingsperiodeNotFoundException -> {
					log.info("Oppfolgingsperiode not found for $arenaId in table $arenaTableName: '${e.message}'")
					arenaDataRepository.upsert(msg.toUpsertInput(arenaId, ingestStatus = IngestStatus.RETRY, note = e.message))
				}
				else -> {
					log.error("$arenaId in table $arenaTableName: ${e.message}", e)
					arenaDataRepository.upsert(msg.toUpsertInput(arenaId, ingestStatus = IngestStatus.RETRY, note = e.message))
				}
			}
		}
	}

	private inline fun <reified D> toArenaKafkaMessage(messageDto: ArenaKafkaMessageDto): ArenaKafkaMessage<D> {
		return ArenaKafkaMessage(
			arenaTableName = messageDto.table,
			operationType = Operation.fromArenaOperationString(messageDto.opType),
			operationTimestamp = parseArenaDateTime(messageDto.opTs),
			operationPosition = messageDto.pos,
			before = messageDto.before?.let { mapper.treeToValue(it, D::class.java) },
			after =  messageDto.after?.let { mapper.treeToValue(it, D::class.java) }
		)
	}

	private fun findProcessorName(arenaTableName: String): String {
		return when(arenaTableName) {
			ARENA_TILTAK_TABLE_NAME -> "tiltak"
			ARENA_GJENNOMFORING_TABLE_NAME -> "gjennomforing"
			ARENA_DELTAKER_TABLE_NAME -> "deltaker"
			else -> "unknown"
		}
	}

	private fun withTimer(processorName: String, runnable: () -> Unit) {
		val timer = meterRegistry.timer(
			"amt.arena-acl.ingestStatus",
			listOf(Tag.of("processor", processorName))
		)

		timer.record { runnable.invoke() }
	}

}
