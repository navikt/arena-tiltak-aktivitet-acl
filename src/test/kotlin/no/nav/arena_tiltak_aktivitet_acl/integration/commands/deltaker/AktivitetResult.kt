package no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker

import no.nav.arena_tiltak_aktivitet_acl.domain.db.ArenaDataDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.db.TranslationDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Aktivitetskort
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetskortHeaders
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.KafkaMessageDto
import org.junit.jupiter.api.fail

class HandledResult(
	position: String,
	arenaDataDbo: ArenaDataDbo,
	translation: TranslationDbo,
	val output: KafkaMessageDto,
	val headers: AktivitetskortHeaders
): AktivitetResult(position, arenaDataDbo, translation) {
	fun output(check: (data: KafkaMessageDto) -> Unit): AktivitetResult {
		check(output)
		return this
	}
	fun aktivitetskort(check: (aktivitetskort: Aktivitetskort) -> Unit): AktivitetResult {
		check(output.aktivitetskort)
		return this
	}
	override fun result(check: (arenaDataDbo: ArenaDataDbo, translation: TranslationDbo?, output: KafkaMessageDto?) -> Unit): AktivitetResult {
		check(arenaDataDbo, translation, output)
		return this
	}
}

open class AktivitetResult(
	val position: String,
	val arenaDataDbo: ArenaDataDbo,
	val translation: TranslationDbo?
) {
	fun expectHandled(check: (data: HandledResult) -> Unit) {
		if (this !is HandledResult) fail("Expected arena message to have ingest status handled")
		check(this)
	}
	fun arenaData(check: (data: ArenaDataDbo) -> Unit): AktivitetResult {
		check.invoke(arenaDataDbo)
		return this
	}

	fun translation(check: (data: TranslationDbo) -> Unit): AktivitetResult {
		if (translation == null) fail("Trying to get translation, but it is null")
		check(translation)
		return this
	}

	open fun result(check: (arenaDataDbo: ArenaDataDbo, translation: TranslationDbo?, output: KafkaMessageDto?) -> Unit): AktivitetResult {
		check(arenaDataDbo, translation, null)
		return this
	}
}
