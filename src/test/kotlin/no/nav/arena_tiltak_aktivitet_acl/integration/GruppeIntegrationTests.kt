package no.nav.arena_tiltak_aktivitet_acl.integration

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.ARENAIDENT
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.gruppetiltak.GruppeTiltak
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.gruppetiltak.GruppeTiltakCommand
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class GruppeIntegrationTests : IntegrationTestBase() {

	fun gruppeTiltak(): GruppeTiltak {
		val now = LocalDateTime.now()
		return GruppeTiltak(
			arenaAktivitetId = 1212,
			tittel = "Gruppe AMO",
			aktivitetstype = "INGVAL",
			beskrivelse = "Du må møte",
			datoFra = now.toLocalDate(),
			datoTil = now.toLocalDate(),
			endretAv = "SIG0331",
			endretTid = now,
			motePlan = emptyList(),
			opprettetAv = "SIG0331",
			opprettetTid = now,
			personIdent = "03011245454"
		)
	}

	@Test
	fun `skal være nice`() {
		val gruppeTiltak = gruppeTiltak()
		val gruppeTiltakCommand = GruppeTiltakCommand(gruppeTiltak)
		gruppeTiltakExecutor.execute(gruppeTiltakCommand)
			.expectHandled {
				withClue("Tittel skal være lik gruppetiltak tittel") {
					it.aktivitetskort.tittel shouldBe gruppeTiltak.tittel
				}
				withClue("beskrivelse.verdi skal være gruppetiltak sin beskrivelse") {
					it.aktivitetskort.beskrivelse?.verdi shouldBe gruppeTiltak.beskrivelse
				}
				withClue("Opprettet tid skal være riktig") {
					it.aktivitetskort.endretTidspunkt shouldBe gruppeTiltak.opprettetTid.truncatedTo(ChronoUnit.SECONDS)
				}
				withClue("Opprettet av skal være riktig") {
					it.aktivitetskort.endretAv.ident shouldBe gruppeTiltak.opprettetAv
					it.aktivitetskort.endretAv.identType shouldBe ARENAIDENT
				}
				withClue("PersonIdent skal være riktig") {
					it.aktivitetskort.personIdent shouldBe gruppeTiltak.personIdent
				}
				withClue("Arena-aktivitetId skal være riktig") {
					it.headers.arenaId shouldBe "ARENAGA${gruppeTiltak.arenaAktivitetId}"
				}
			}
	}

//	fun asfsadf() {
//		AKTIVITET_DATO_PASSERT -> oppdater status (Fullført)
//		STATUS_ENDERT(AVBR, FULLF) -> oppdater status
//		if (opretation == Operation.DELETED) {
//			DELTAKER_FJERNET
//			// Møteplan
//			MØTEPLAN_SLETTET
//			MØTEPLAN_DATO_PASSERT(etter å ha blitt endret)
//			MØTEPLAN_DATO_PASSERT(uten endring)
//		}
//		moter.erAllePassert() -> (oppdater tekst)
//	}

}
