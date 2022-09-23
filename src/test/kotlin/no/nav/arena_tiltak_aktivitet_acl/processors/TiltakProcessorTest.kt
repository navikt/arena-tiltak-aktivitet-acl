package no.nav.arena_tiltak_aktivitet_acl.processors

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.arena_tiltak_aktivitet_acl.database.DatabaseTestUtils
import no.nav.arena_tiltak_aktivitet_acl.database.SingletonPostgresContainer
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaTiltak
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaTiltakKafkaMessage
import no.nav.arena_tiltak_aktivitet_acl.exceptions.OperationNotImplementedException
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.TiltakRepository
import no.nav.arena_tiltak_aktivitet_acl.services.TiltakService
import no.nav.arena_tiltak_aktivitet_acl.utils.ARENA_TILTAK_TABLE_NAME
import no.nav.arena_tiltak_aktivitet_acl.utils.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDateTime
import java.util.*

class TiltakProcessorTest : FunSpec({

	val dataSource = SingletonPostgresContainer.getDataSource()

	val mapper = ObjectMapper.get()

	lateinit var arenaDataRepository: ArenaDataRepository
	lateinit var tiltakRepository: TiltakRepository

	lateinit var tiltakProcessor: TiltakProcessor

	beforeEach {
		val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
		rootLogger.level = Level.WARN

		val template = NamedParameterJdbcTemplate(dataSource)

		arenaDataRepository = ArenaDataRepository(template)
		tiltakRepository = TiltakRepository(template)
		DatabaseTestUtils.cleanDatabase(dataSource)

		tiltakProcessor = TiltakProcessor(arenaDataRepository, TiltakService(tiltakRepository))
	}

	test("Add Tiltak") {
		val position = UUID.randomUUID().toString()
		val tiltakKode = "Tiltak1_KODE"
		val tiltakNavn = "Tiltak1_NAVN"

		val data = createArenaTiltakKafkaMessage(
			operationPosition = position,
			arenaTiltak = createArenaTiltak(tiltakNavn, tiltakKode)
		)

		tiltakProcessor.handleArenaMessage(data)

		val arenaDataRepositoryEntry = shouldNotThrowAny {
			arenaDataRepository.get(ARENA_TILTAK_TABLE_NAME, Operation.CREATED, position)
		}

		arenaDataRepositoryEntry.before shouldBe null
		mapper.readValue(arenaDataRepositoryEntry.after, ArenaTiltak::class.java) shouldBe data.after
		arenaDataRepositoryEntry.operation shouldBe Operation.CREATED
		arenaDataRepositoryEntry.id shouldNotBe -1

		arenaDataRepositoryEntry.ingestStatus shouldBe IngestStatus.HANDLED
		arenaDataRepositoryEntry.ingestedTimestamp shouldNotBe null
		arenaDataRepositoryEntry.ingestAttempts shouldBe 0


		val tiltakRepositoryEntry = tiltakRepository.getByKode(tiltakKode)

		tiltakRepositoryEntry shouldNotBe null
		tiltakRepositoryEntry!!.id shouldNotBe null
		tiltakRepositoryEntry.kode shouldBe tiltakKode
		tiltakRepositoryEntry.navn shouldBe tiltakNavn
	}

	test("Update Tiltak") {
		val newPosition = UUID.randomUUID().toString()
		val tiltakKode = "Tiltak1_KODE"
		val tiltakNavn = "Tiltak1_NAVN"

		val kafkaMessageInsertOp = createArenaTiltakKafkaMessage(
			operationPosition = newPosition,
			operationType = Operation.CREATED,
			arenaTiltak = createArenaTiltak(tiltakNavn, tiltakKode)
		)

		tiltakProcessor.handleArenaMessage(kafkaMessageInsertOp)

		val updatedPosition = UUID.randomUUID().toString()
		val updatedNavn = "TILTAK1_UPDATED_NAVN"

		val kafkaMessageUpdateOp = createArenaTiltakKafkaMessage(
			operationPosition = updatedPosition,
			operationType = Operation.MODIFIED,
			arenaTiltak = createArenaTiltak(updatedNavn, tiltakKode)
		)

		tiltakProcessor.handleArenaMessage(kafkaMessageUpdateOp)

		val arenaDataRepositoryEntry = shouldNotThrowAny {
			arenaDataRepository.get(ARENA_TILTAK_TABLE_NAME, Operation.MODIFIED, updatedPosition)
		}

		arenaDataRepositoryEntry.operation shouldBe Operation.MODIFIED
		arenaDataRepositoryEntry.ingestStatus shouldBe IngestStatus.HANDLED
		arenaDataRepositoryEntry.ingestedTimestamp shouldNotBe null
		arenaDataRepositoryEntry.ingestAttempts shouldBe 0


		val tiltakRepositoryEntry = tiltakRepository.getByKode(tiltakKode)

		tiltakRepositoryEntry shouldNotBe null
		tiltakRepositoryEntry!!.id shouldNotBe null
		tiltakRepositoryEntry.kode shouldBe tiltakKode
		tiltakRepositoryEntry.navn shouldBe updatedNavn
	}

	test("Delete Tiltak should throw exception") {
		val newPosition = UUID.randomUUID().toString()
		val tiltakKode = "Tiltak1_KODE"
		val tiltakNavn = "Tiltak1_NAVN"

		val kafkaMessageInsertOp = createArenaTiltakKafkaMessage(
			operationPosition = newPosition,
			operationType = Operation.CREATED,
			arenaTiltak = createArenaTiltak(tiltakNavn, tiltakKode)
		)

		tiltakProcessor.handleArenaMessage(kafkaMessageInsertOp)

		val deletePosition = UUID.randomUUID().toString()

		val kafkaMessageDeleteOp = createArenaTiltakKafkaMessage(
			operationPosition = deletePosition,
			operationType = Operation.DELETED,
			arenaTiltak = createArenaTiltak(tiltakNavn, tiltakKode)
		)

		shouldThrowExactly<OperationNotImplementedException> {
			tiltakProcessor.handleArenaMessage(kafkaMessageDeleteOp)
		}
	}

})

private fun createArenaTiltak(
	tiltakNavn: String,
	tiltakKode: String
): ArenaTiltak {
	return ArenaTiltak(
		TILTAKSNAVN = tiltakNavn,
		TILTAKSGRUPPEKODE = "",
		REG_DATO = "",
		REG_USER = "",
		MOD_DATO = "",
		MOD_USER = "",
		TILTAKSKODE = tiltakKode,
		DATO_FRA = "",
		DATO_TIL = "",
		STATUS_BASISYTELSE = "",
		ADMINISTRASJONKODE = "",
		STATUS_KOPI_TILSAGN = "",
		ARKIVNOKKEL = "",
		STATUS_ANSKAFFELSE = "",
		MAKS_ANT_SOKERE = 0,
		STATUS_KALKULATOR = "",
		RAMMEAVTALE = "",
		HANDLINGSPLAN = "",
		STATUS_SLUTTDATO = "",
		STATUS_VEDTAK = "",
		STATUS_IA_AVTALE = "",
		STATUS_TILLEGGSSTONADER = "",
		STATUS_UTDANNING = "",
		AUTOMATISK_TILSAGNSBREV = "",
		STATUS_BEGRUNNELSE_INNSOKT = "",
		STATUS_HENVISNING_BREV = "",
		STATUS_KOPIBREV = "",
	)
}

private fun createArenaTiltakKafkaMessage(
	operationPosition: String = "1",
	operationType: Operation = Operation.CREATED,
	operationTimestamp: LocalDateTime = LocalDateTime.now(),
	arenaTiltak: ArenaTiltak,
): ArenaTiltakKafkaMessage {
	return ArenaTiltakKafkaMessage(
		arenaTableName =  ARENA_TILTAK_TABLE_NAME,
		operationType = operationType,
		operationTimestamp = operationTimestamp,
		operationPosition =  operationPosition,
		before = if (listOf(Operation.MODIFIED, Operation.DELETED).contains(operationType)) arenaTiltak else null,
		after =  if (listOf(Operation.CREATED, Operation.MODIFIED).contains(operationType)) arenaTiltak else null,
	)
}
