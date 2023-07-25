package no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker

import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.db.TranslationDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Aktivitetskort
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetskortHeaders
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.KafkaMessageDto
import org.junit.jupiter.api.fail

data class AktivitetResult(
	val position: String,
	val arenaDataDbo: ArenaDataDbo,
	val translation: TranslationDbo?,
	val output: KafkaMessageDto?,
	val headers: AktivitetskortHeaders?
) {
	fun arenaData(check: (data: ArenaDataDbo) -> Unit): AktivitetResult {
		check.invoke(arenaDataDbo)
		return this
	}

	fun translation(check: (data: TranslationDbo) -> Unit): AktivitetResult {
		if (translation == null) {
			fail("Trying to get translation, but it is null")
		}

		check.invoke(translation)
		return this
	}

	fun output(check: (data: KafkaMessageDto) -> Unit): AktivitetResult {
		if (output == null) {
			fail("Trying to get output, but it is null")
		}

		check.invoke(output)
		return this
	}

	fun result(check: (arenaDataDbo: ArenaDataDbo, translation: TranslationDbo?, output: KafkaMessageDto?) -> Unit): AktivitetResult {
		check.invoke(arenaDataDbo, translation, output)
		return this
	}

	fun outgoingPayload(check: (aktivitetskort: Aktivitetskort) -> Unit): AktivitetResult {
		if (output?.aktivitetskort == null) {
			fail("Forsøker å hente payload på en outgoing melding som er null")
		}

		check.invoke(output.aktivitetskort)
		return this
	}
}
