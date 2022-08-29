package no.nav.arena_tiltak_aktivitet_acl.integration

import io.kotest.matchers.shouldBe
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Aktivitet
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker.DeltakerInput
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker.NyDeltakerCommand
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.gjennomforing.GjennomforingInput
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.gjennomforing.NyGjennomforingCommand
import no.nav.arena_tiltak_aktivitet_acl.processors.converters.ArenaDeltakerStatusConverter
import org.junit.jupiter.api.Test
import java.util.*

class DeltakerIntegrationTests : IntegrationTestBase() {

	@Test
	fun `ingest deltaker`() {
		val gjennomforingId = Random().nextLong()
		val deltakerId = Random().nextLong()

		val gjennomforingInput = GjennomforingInput(gjennomforingId = gjennomforingId)

		gjennomforingExecutor.execute(NyGjennomforingCommand(gjennomforingInput))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }

		val deltakerInput = DeltakerInput(
			tiltakDeltakerId = deltakerId,
			tiltakgjennomforingId = gjennomforingId,
			innsokBegrunnelse = "innsøkbegrunnelse"
		)

		val result = deltakerExecutor.execute(NyDeltakerCommand(deltakerInput))
		println(result)
		result.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.output { it.operation shouldBe Operation.CREATED }
			.result { _, translation, output -> translation!!.aktivitetId shouldBe output!!.payload!!.id }
			.outgoingPayload { it.isSame(deltakerInput, gjennomforingInput) }
	}

	fun Aktivitet.isSame(deltakerInput: DeltakerInput, gjennomforingInput: GjennomforingInput) {
		//this.personIdent shouldBe ??
		tittel shouldBe gjennomforingInput.navn
		type shouldBe Aktivitet.Type.TILTAKSAKTIVITET
		tiltakKode shouldBe gjennomforingInput.tiltakKode
		status shouldBe ArenaDeltakerStatusConverter.toAktivitetStatus(deltakerInput.deltakerStatusKode)
		startDato shouldBe deltakerInput.datoFra
		sluttDato shouldBe deltakerInput.datoTil
		//this.arrangorNavn shouldBe  ??
		deltakelseProsent shouldBe deltakerInput.prosentDeltid
		dagerPerUke shouldBe deltakerInput.antallDagerPerUke
		beskrivelse shouldBe gjennomforingInput.navn
		//registrertDato shouldBe deltakerInput.registrertDato datetime precision
		statusEndretDato shouldBe deltakerInput.datoStatusEndring.atStartOfDay()

	}
}

