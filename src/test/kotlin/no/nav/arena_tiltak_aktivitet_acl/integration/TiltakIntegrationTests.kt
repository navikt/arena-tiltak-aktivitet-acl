package no.nav.arena_tiltak_aktivitet_acl.integration

import io.kotest.matchers.shouldBe
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.tiltak.NyttTiltakCommand
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.tiltak.OppdaterTiltakCommand
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.tiltak.SlettTiltakCommand
import org.junit.jupiter.api.Test
import java.util.*

class TiltakIntegrationTests : IntegrationTestBase() {

	@Test
	fun leggTilNyttTiltak() {
		val kode = UUID.randomUUID().toString()
		val navn = UUID.randomUUID().toString()

		tiltakExecutor.execute(NyttTiltakCommand(kode, navn))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.tiltak { it.kode shouldBe kode }
			.tiltak { it.navn shouldBe navn }
	}

	@Test
	fun oppdaterTiltakOppdatererTiltakIDatabasen() {
		val kode = UUID.randomUUID().toString()
		val navn = UUID.randomUUID().toString()
		val oppdatertNavn = UUID.randomUUID().toString()

		tiltakExecutor.execute(NyttTiltakCommand(kode, navn))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }

		tiltakExecutor.execute(OppdaterTiltakCommand(kode, navn, oppdatertNavn))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.tiltak { it.kode shouldBe kode }
			.tiltak { it.navn shouldBe oppdatertNavn }
	}

	@Test
	fun slettTiltakFeilerOgLeggerNotatPaArenaData() {
		val kode = UUID.randomUUID().toString()
		val navn = UUID.randomUUID().toString()

		tiltakExecutor.execute(NyttTiltakCommand(kode, navn))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }

		tiltakExecutor.execute(SlettTiltakCommand(kode, navn))
			.arenaData { it.ingestStatus shouldBe IngestStatus.FAILED }
			.arenaData { it.note shouldBe "Kan ikke håndtere tiltak med operation type DELETE" }
			.tiltak { it.kode shouldBe kode }
			.tiltak { it.navn shouldBe navn }
	}

}
