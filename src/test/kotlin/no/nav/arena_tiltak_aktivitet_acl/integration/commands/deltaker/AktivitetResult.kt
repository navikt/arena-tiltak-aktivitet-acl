package no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker

import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.db.TranslationDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.TiltakAktivitet
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.KafkaMessageDto
import org.junit.jupiter.api.fail

data class AktivitetResult(
	val position: String,
	val arenaDataDbo: ArenaDataDbo,
	val translation: TranslationDbo?,
	val output: KafkaMessageDto<TiltakAktivitet>?
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

	fun output(check: (data: KafkaMessageDto<TiltakAktivitet>) -> Unit): AktivitetResult {
		if (output == null) {
			fail("Trying to get output, but it is null")
		}

		check.invoke(output)
		return this
	}

	fun result(check: (arenaDataDbo: ArenaDataDbo, translation: TranslationDbo?, output: KafkaMessageDto<TiltakAktivitet>?) -> Unit): AktivitetResult {
		check.invoke(arenaDataDbo, translation, output)
		return this
	}

	fun outgoingPayload(check: (payload: TiltakAktivitet) -> Unit): AktivitetResult {
		if (output?.payload == null) {
			fail("Forsøker å hente payload på en outgoing melding som er null")
		}

		check.invoke(output.payload!!)
		return this
	}
}
