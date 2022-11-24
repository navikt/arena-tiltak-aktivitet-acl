package no.nav.arena_tiltak_aktivitet_acl.integration

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.*
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker.AktivitetResult
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker.DeltakerInput
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker.NyDeltakerCommand
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.gjennomforing.GjennomforingInput
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.gjennomforing.NyGjennomforingCommand
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.tiltak.NyttTiltakCommand
import no.nav.arena_tiltak_aktivitet_acl.processors.DeltakerProcessor
import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.TranslationRepository
import no.nav.arena_tiltak_aktivitet_acl.utils.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.*

class DeltakerIntegrationTests : IntegrationTestBase() {

	@Autowired
	lateinit var aktivitetRepository: AktivitetRepository

	@Autowired
	lateinit var arenaDataRepository: ArenaDataRepository

	@Autowired
	lateinit var translationRepository: TranslationRepository

	data class TestData (
		val gjennomforingId: Long = Random().nextLong(),
		val deltakerId: Long = Random().nextLong(),
		val gjennomforingInput: GjennomforingInput = GjennomforingInput(gjennomforingId = gjennomforingId)
	)

	private fun setup(): TestData {
		val testData = TestData()

		tiltakExecutor.execute(NyttTiltakCommand())
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }

		gjennomforingExecutor.execute(NyGjennomforingCommand(testData.gjennomforingInput))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }

		return testData
	}

	@Test
	fun `ingest deltaker`() {
		val (gjennomforingId, deltakerId, gjennomforingInput) = setup()

		val deltakerInput = DeltakerInput(
			tiltakDeltakerId = deltakerId,
			tiltakgjennomforingId = gjennomforingId,
			innsokBegrunnelse = "innsøkbegrunnelse",
			endretAv = Ident(ident = "SIG123"),
		)
		val deltakerCommand = NyDeltakerCommand(deltakerInput)
		val result = deltakerExecutor.execute(deltakerCommand)

		result.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.output { it.actionType shouldBe ActionType.UPSERT_AKTIVITETSKORT_V1 }
			.result { _, translation, output -> translation!!.aktivitetId shouldBe output!!.aktivitetskort.id }
			.outgoingPayload { it.isSame(deltakerInput, gjennomforingInput) }
	}


	@Test
	fun `process deltakelse in the correct order`() {
		val (gjennomforingId, deltakerId, gjennomforingInput) = TestData()

		val deltakerInput = DeltakerInput(
			tiltakDeltakerId = deltakerId,
			tiltakgjennomforingId = gjennomforingId,
			innsokBegrunnelse = "innsøkbegrunnelse",
			deltakerStatusKode = "INFOMOETE", // Aktivitetstatus: Planlagt
			endretAv = Ident(ident = "SIG123"),
		)
		val deltakerCommand = NyDeltakerCommand(deltakerInput)
		val result: AktivitetResult = deltakerExecutor.execute(deltakerCommand)

		result.arenaData { it.ingestStatus shouldBe IngestStatus.RETRY }


		val deltakerCommand2 = NyDeltakerCommand(deltakerInput.copy(deltakerStatusKode = "GJENN"))
		val result2: AktivitetResult = deltakerExecutor.execute(deltakerCommand2)

		result2.arenaData { it.ingestStatus shouldBe IngestStatus.QUEUED }

		tiltakExecutor.execute(NyttTiltakCommand())
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }

		gjennomforingExecutor.execute(NyGjennomforingCommand(gjennomforingInput))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }

		val deltakerCommand3 = NyDeltakerCommand(deltakerInput.copy(deltakerStatusKode = "FULLF"))
		val result3: AktivitetResult = deltakerExecutor.execute(deltakerCommand3)

		result3.arenaData { it.ingestStatus shouldBe IngestStatus.QUEUED }

		// Cron-job
		processMessages()

		val aktivitetId = translationRepository.get(deltakerId, AktivitetKategori.TILTAKSAKTIVITET)?.aktivitetId
		aktivitetId shouldNotBe null

		val mapper = ObjectMapper.get()
		val data = aktivitetRepository.getAktivitet(aktivitetId!!)!!.data
		val aktivitetskort = mapper.readValue(data, Aktivitetskort::class.java)
		aktivitetskort.aktivitetStatus shouldBe AktivitetStatus.PLANLAGT

		processMessages()

		val data2 = aktivitetRepository.getAktivitet(aktivitetId)!!.data
		val aktivitetskort2 = mapper.readValue(data2, Aktivitetskort::class.java)
		aktivitetskort2.aktivitetStatus shouldBe AktivitetStatus.GJENNOMFORES

		processMessages()

		val data3 = aktivitetRepository.getAktivitet(aktivitetId)!!.data
		val aktivitetskort3 = mapper.readValue(data3, Aktivitetskort::class.java)
		aktivitetskort3.aktivitetStatus shouldBe AktivitetStatus.FULLFORT
	}

	@Test
	fun `process deltakelse in the correct order also when failed`() {
		val (gjennomforingId, deltakerId, gjennomforingInput) = TestData()

		val deltakerInput = DeltakerInput(
			tiltakDeltakerId = deltakerId,
			tiltakgjennomforingId = gjennomforingId,
			innsokBegrunnelse = "innsøkbegrunnelse",
			deltakerStatusKode = "INFOMOETE", // Aktivitetstatus: Planlagt
			endretAv = Ident(ident = "SIG123"),
		)
		val deltakerCommand = NyDeltakerCommand(deltakerInput)
		val result: AktivitetResult = deltakerExecutor.execute(deltakerCommand)

		result.arenaData { it.ingestStatus shouldBe IngestStatus.RETRY }

		// Fail first message after 10 retries
		(1..10).forEach{ processMessages() }
		val resultDbo = arenaDataRepository.get(result.arenaDataDbo.arenaTableName, result.arenaDataDbo.operation, result.arenaDataDbo.operationPosition )
		resultDbo.ingestStatus shouldBe IngestStatus.FAILED

		val deltakerCommand2 = NyDeltakerCommand(deltakerInput.copy(deltakerStatusKode = "GJENN"))
		val result2: AktivitetResult = deltakerExecutor.execute(deltakerCommand2)

		result2.arenaData { it.ingestStatus shouldBe IngestStatus.QUEUED }

		tiltakExecutor.execute(NyttTiltakCommand())
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }

		gjennomforingExecutor.execute(NyGjennomforingCommand(gjennomforingInput))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }

		val deltakerCommand3 = NyDeltakerCommand(deltakerInput.copy(deltakerStatusKode = "FULLF"))
		val result3: AktivitetResult = deltakerExecutor.execute(deltakerCommand3)

		result3.arenaData { it.ingestStatus shouldBe IngestStatus.QUEUED }

		// Cron-job
		processFailedMessages()

		val aktivitetId = translationRepository.get(deltakerId, AktivitetKategori.TILTAKSAKTIVITET)?.aktivitetId
		aktivitetId shouldNotBe null

		val mapper = ObjectMapper.get()
		val data = aktivitetRepository.getAktivitet(aktivitetId!!)!!.data
		val aktivitetskort = mapper.readValue(data, Aktivitetskort::class.java)
		aktivitetskort.aktivitetStatus shouldBe AktivitetStatus.PLANLAGT

		processMessages()

		val data2 = aktivitetRepository.getAktivitet(aktivitetId)!!.data
		val aktivitetskort2 = mapper.readValue(data2, Aktivitetskort::class.java)
		aktivitetskort2.aktivitetStatus shouldBe AktivitetStatus.GJENNOMFORES

		processMessages()

		val data3 = aktivitetRepository.getAktivitet(aktivitetId)!!.data
		val aktivitetskort3 = mapper.readValue(data3, Aktivitetskort::class.java)
		aktivitetskort3.aktivitetStatus shouldBe AktivitetStatus.FULLFORT
	}
	@Test
	fun `ingest existing deltaker`() {
		val (gjennomforingId, deltakerId, gjennomforingInput) = setup()

		val deltakerInput = DeltakerInput(
			tiltakDeltakerId = deltakerId,
			tiltakgjennomforingId = gjennomforingId,
			innsokBegrunnelse = "innsøkbegrunnelse",
			datoFra = LocalDate.now().minusDays(10),
			endretAv = Ident(ident = "SIG123"),
		)
		val deltakerCommand = NyDeltakerCommand(deltakerInput)
		val result = deltakerExecutor.execute(deltakerCommand)

		val deltakerInputUpdated = DeltakerInput(
			tiltakDeltakerId = deltakerId,
			tiltakgjennomforingId = gjennomforingId,
			innsokBegrunnelse = "innsøkbegrunnelse",
			datoTil = LocalDate.now().plusDays(30),
			endretAv = Ident(ident = "SIG123"),
		)
		val updatedDeltakerCommand = NyDeltakerCommand(deltakerInputUpdated)
		val updatedResult = deltakerExecutor.execute(updatedDeltakerCommand)

		result.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.output { it.actionType shouldBe ActionType.UPSERT_AKTIVITETSKORT_V1 }
			.result { _, translation, output -> translation!!.aktivitetId shouldBe output!!.aktivitetskort.id }
			.outgoingPayload { it.isSame(deltakerInput, gjennomforingInput) }

		updatedResult.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.output { it.actionType shouldBe ActionType.UPSERT_AKTIVITETSKORT_V1 }
			.result { _, translation, output -> translation!!.aktivitetId shouldBe output!!.aktivitetskort.id }
			.outgoingPayload { it.isSame(deltakerInputUpdated, gjennomforingInput) }
	}

	@Test
	fun `ignore deltaker before aktivitetsplan launch`() {
		val (gjennomforingId, deltakerId) = setup()

		val deltakerInput = DeltakerInput(
			tiltakDeltakerId = deltakerId,
			tiltakgjennomforingId = gjennomforingId,
			innsokBegrunnelse = "innsøkbegrunnelse",
			endretAv = Ident(ident = "SIG123"),
			registrertDato = DeltakerProcessor.AKTIVITETSPLAN_LANSERINGSDATO.minusDays(1)
		)
		val deltakerCommand = NyDeltakerCommand(deltakerInput)
		val result = deltakerExecutor.execute(deltakerCommand)

		result.arenaData { it.ingestStatus shouldBe IngestStatus.IGNORED }
	}

	@Test
	fun `tittel should be set to tiltaksnavn when gjennomforing navn is null`() {
		val TILTAKSNAVN_OVERRIDE = "Tiltaksnavn override"

		val gjennomforingId: Long = Random().nextLong()
		val deltakerId: Long = Random().nextLong()
		val gjennomforingInput = GjennomforingInput(
			gjennomforingId = gjennomforingId,
			navn = null
		)

		tiltakExecutor.execute(NyttTiltakCommand(navn = TILTAKSNAVN_OVERRIDE))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }

		gjennomforingExecutor.execute(NyGjennomforingCommand(gjennomforingInput))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }


		val deltakerInput = DeltakerInput(
			tiltakDeltakerId = deltakerId,
			tiltakgjennomforingId = gjennomforingId,
			innsokBegrunnelse = "innsøkbegrunnelse",
			endretAv = Ident(ident = "SIG123")
		)

		val deltakerCommand = NyDeltakerCommand(deltakerInput)
		val result = deltakerExecutor.execute(deltakerCommand)

		result.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.output { it.actionType shouldBe ActionType.UPSERT_AKTIVITETSKORT_V1 }
			.result { _, _, output -> output?.aktivitetskort?.tittel shouldBe TILTAKSNAVN_OVERRIDE }
			.outgoingPayload { it.isSame(deltakerInput, gjennomforingInput.copy(navn = TILTAKSNAVN_OVERRIDE)) }

	}

	private fun Aktivitetskort.isSame(deltakerInput: DeltakerInput, gjennomforingInput: GjennomforingInput) {
		personIdent shouldBe "12345"
		tittel shouldBe gjennomforingInput.navn
		aktivitetStatus shouldBe AktivitetStatus.GJENNOMFORES
		etiketter.size shouldBe 0
		startDato shouldBe deltakerInput.datoFra
		sluttDato shouldBe deltakerInput.datoTil
		beskrivelse shouldBe null
		detaljer[0].verdi shouldBe "virksomhetnavn"
		detaljer[1].verdi shouldBe "${deltakerInput.prosentDeltid}%"
		detaljer[2].verdi shouldBe deltakerInput.antallDagerPerUke.toString()
		endretAv shouldBe deltakerInput.endretAv
	}
}

