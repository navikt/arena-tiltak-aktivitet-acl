package no.nav.arena_tiltak_aktivitet_acl.integration

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.ActionType
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.TiltakAktivitet
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker.DeltakerInput
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker.NyDeltakerCommand
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.gjennomforing.GjennomforingInput
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.gjennomforing.NyGjennomforingCommand
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.tiltak.NyttTiltakCommand
import org.junit.jupiter.api.Test
import java.util.*

class DeltakerIntegrationTests : IntegrationTestBase() {

	@Test
	fun `ingest deltaker`() {
		val gjennomforingId = Random().nextLong()
		val deltakerId = Random().nextLong()

		val gjennomforingInput = GjennomforingInput(gjennomforingId = gjennomforingId)

		tiltakExecutor.execute(NyttTiltakCommand())
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }

		gjennomforingExecutor.execute(NyGjennomforingCommand(gjennomforingInput))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }

		val deltakerInput = DeltakerInput(
			tiltakDeltakerId = deltakerId,
			tiltakgjennomforingId = gjennomforingId,
			innsokBegrunnelse = "innsøkbegrunnelse",
			endretAv = "SIG123"
		)
		val deltakerCommand = NyDeltakerCommand(deltakerInput)
		val result = deltakerExecutor.execute(deltakerCommand)
		println(result)
		result.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.output { it.actionType shouldBe ActionType.UPSERT_TILTAK_AKTIVITET_V1 }
			.result { _, translation, output -> translation!!.aktivitetId shouldBe output!!.payload.id }
			.outgoingPayload { it.isSame(deltakerInput, gjennomforingInput) }
	}

	private fun TiltakAktivitet.isSame(deltakerInput: DeltakerInput, gjennomforingInput: GjennomforingInput) {
		personIdent shouldBe "12345"
		eksternReferanseId shouldBe deltakerInput.tiltakDeltakerId
		tittel shouldBe gjennomforingInput.navn
		tiltak.kode shouldBe gjennomforingInput.tiltakKode
		status.type shouldBe AktivitetStatus.GJENNOMFORES
		status.aarsak shouldBe null
		startDato shouldBe deltakerInput.datoFra
		sluttDato shouldBe deltakerInput.datoTil
		beskrivelse shouldBe null
		arrangorNavn shouldBe "virksomhetnavn"
		deltakelseProsent shouldBe deltakerInput.prosentDeltid
		dagerPerUke shouldBe deltakerInput.antallDagerPerUke
		registrertDato shouldNotBe null
		statusEndretDato shouldBe deltakerInput.datoStatusEndring.atStartOfDay()
		endretAv shouldBe deltakerInput.endretAv
	}
}

