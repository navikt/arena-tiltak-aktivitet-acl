package no.nav.arena_tiltak_aktivitet_acl.integration

import io.kotest.matchers.shouldBe
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.ActionType
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Aktivitetskort
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker.DeltakerInput
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker.NyDeltakerCommand
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.gjennomforing.GjennomforingInput
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.gjennomforing.NyGjennomforingCommand
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.tiltak.NyttTiltakCommand
import org.junit.jupiter.api.Test
import java.util.*
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Ident

class DeltakerIntegrationTests : IntegrationTestBase() {

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
			.output { it.actionType shouldBe ActionType.UPSERT_TILTAK_AKTIVITET_V1 }
			.result { _, translation, output -> translation!!.aktivitetId shouldBe output!!.payload.id }
			.outgoingPayload { it.isSame(deltakerInput, gjennomforingInput) }
	}

	@Test
	fun `ingest existing deltaker`() {
		val (gjennomforingId, deltakerId, gjennomforingInput) = setup()

		val deltakerInput = DeltakerInput(
			tiltakDeltakerId = deltakerId,
			tiltakgjennomforingId = gjennomforingId,
			innsokBegrunnelse = "innsøkbegrunnelse",
			endretAv = Ident(ident = "SIG123"),
		)
		val deltakerCommand = NyDeltakerCommand(deltakerInput)
		val result = deltakerExecutor.execute(deltakerCommand)

		val deltakerInputUpdated = DeltakerInput(
			tiltakDeltakerId = deltakerId,
			tiltakgjennomforingId = gjennomforingId,
			innsokBegrunnelse = "innsøkbegrunnelse",
			endretAv = Ident(ident = "SIG123"),
		)
		val updatedDeltakerCommand = NyDeltakerCommand(deltakerInput)
		val updatedResult = deltakerExecutor.execute(deltakerCommand)

		result.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.output { it.actionType shouldBe ActionType.UPSERT_TILTAK_AKTIVITET_V1 }
			.result { _, translation, output -> translation!!.aktivitetId shouldBe output!!.payload.id }
			.outgoingPayload { it.isSame(deltakerInput, gjennomforingInput) }
	}

	private fun Aktivitetskort.isSame(deltakerInput: DeltakerInput, gjennomforingInput: GjennomforingInput) {
		personIdent shouldBe "12345"
		eksternReferanseId shouldBe deltakerInput.tiltakDeltakerId
		tittel shouldBe gjennomforingInput.navn
		aktivitetStatus shouldBe AktivitetStatus.GJENNOMFORES
		tiltaksKode shouldBe gjennomforingInput.tiltakKode
		deltakelseStatus shouldBe null
		startDato shouldBe deltakerInput.datoFra
		sluttDato shouldBe deltakerInput.datoTil
		beskrivelse shouldBe null
		arrangorNavn shouldBe "virksomhetnavn"
		detaljer["deltakelseProsent"] shouldBe deltakerInput.prosentDeltid.toString()
		detaljer["dagerPerUke"] shouldBe deltakerInput.antallDagerPerUke.toString()
		endretAv shouldBe deltakerInput.endretAv
	}
}

