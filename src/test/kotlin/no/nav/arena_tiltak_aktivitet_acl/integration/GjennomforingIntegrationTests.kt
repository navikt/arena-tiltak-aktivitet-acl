package no.nav.arena_tiltak_aktivitet_acl.integration

import io.kotest.matchers.shouldBe
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.gjennomforing.GjennomforingInput
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.gjennomforing.NyGjennomforingCommand
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.tiltak.NyttTiltakCommand
import no.nav.arena_tiltak_aktivitet_acl.mocks.OrdsClientMock
import org.junit.jupiter.api.Test
import java.util.*

class GjennomforingIntegrationTests : IntegrationTestBase() {

	@Test
	fun `Konsumer gjennomføring - gyldig gjennomføring - ingestes uten feil`() {

		val gjennomforingInput = GjennomforingInput(
			gjennomforingId = Random().nextLong()
		)
		val arbgivId = gjennomforingInput.arbeidsgiverIdArrangor
		val virksomhetsnummer = "123"
		val expected = gjennomforingInput.toDbo(gjennomforingInput.gjennomforingId, virksomhetsnummer, "virksomhetnavn")
		OrdsClientMock.virksomhetsHandler[arbgivId!!] = { virksomhetsnummer }

		tiltakExecutor.execute(NyttTiltakCommand())
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }

		gjennomforingExecutor.execute(NyGjennomforingCommand(gjennomforingInput))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.output { it shouldBe expected }
	}

	@Test
	fun `Konsumer gjennomføring - Feilet på første forsøk - Skal settes til RETRY`() {
		val virksomhetsId = 456785618L

		tiltakExecutor.execute(NyttTiltakCommand())

		OrdsClientMock.virksomhetsHandler[virksomhetsId] = { throw RuntimeException() }

		val input = GjennomforingInput(
			gjennomforingId = Random().nextLong(),
			arbeidsgiverIdArrangor = virksomhetsId
		)

		gjennomforingExecutor.execute(NyGjennomforingCommand(input))
			.arenaData { it.ingestStatus shouldBe IngestStatus.RETRY }
			.result { _, output -> output shouldBe null }

	}

	@Test
	fun `Konsumer gjennomføring - lokaltnavn er null - gjennomføringsnavn blir satt til tiltaksnavn i deltakerprocessor`() {
		tiltakExecutor.execute(NyttTiltakCommand())

		val input = GjennomforingInput(
			gjennomforingId = Random().nextLong(),
			navn = null
		)

		gjennomforingExecutor.execute(NyGjennomforingCommand(input))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.result { _, output -> output?.navn shouldBe null }
	}

	@Test
	fun `Konsumer gjennomføring - lokaltnavn har fnr - gjennomføringsnavn skal vaskes`() {
		tiltakExecutor.execute(NyttTiltakCommand())

		val input = GjennomforingInput(
			gjennomforingId = Random().nextLong(),
			navn = "10108094523 Brua frisør"
		)

		gjennomforingExecutor.execute(NyGjennomforingCommand(input))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.result { _, output -> output?.navn shouldBe "[FNR] Brua frisør" }
	}

	@Test
	fun `Konsumer gjennomføring - lokaltnavn har kun spesialkarakterer - gjennomføringsnavn skal vaskes`() {
		tiltakExecutor.execute(NyttTiltakCommand())

		val input = GjennomforingInput(
			gjennomforingId = Random().nextLong(),
			navn = "....-#$%___"
		)

		gjennomforingExecutor.execute(NyGjennomforingCommand(input))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.result { _, output -> output?.navn shouldBe null }
	}
}
