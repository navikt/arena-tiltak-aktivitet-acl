package no.nav.arena_tiltak_aktivitet_acl.integration.executors

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.TiltakAktivitet
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.KafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker.DeltakerCommand
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.deltaker.AktivitetResult
import no.nav.arena_tiltak_aktivitet_acl.integration.kafka.KafkaAktivitetskortIntegrationConsumer
import no.nav.arena_tiltak_aktivitet_acl.repositories.TranslationRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import java.util.*

class DeltakerTestExecutor(
	kafkaProducer: KafkaProducerClientImpl<String, String>,
	arenaDataRepository: ArenaDataRepository,
	translationRepository: TranslationRepository
) : TestExecutor(
	kafkaProducer = kafkaProducer,
	arenaDataRepository = arenaDataRepository,
	translationRepository = translationRepository
) {

	private val topic = "deltaker"
	private val outputMessages = mutableListOf<KafkaMessageDto<TiltakAktivitet>>()

	init {
		KafkaAktivitetskortIntegrationConsumer.subscribeAktivitet { outputMessages.add(it) }
	}

	fun execute(command: DeltakerCommand): AktivitetResult {
		return command.execute(incrementAndGetPosition()) { sendAndCheck(it) }
	}

	fun updateResults(position: String, command: DeltakerCommand): AktivitetResult {
		return command.execute(position) { getResults(it) }
	}

	private fun sendAndCheck(wrapper: ArenaKafkaMessageDto): AktivitetResult {
		sendKafkaMessage(topic, objectMapper.writeValueAsString(wrapper))
		return getResults(wrapper)
	}

	private fun getResults(wrapper: ArenaKafkaMessageDto): AktivitetResult {
		val arenaData = getArenaData(
			ARENA_DELTAKER_TABLE_NAME,
			Operation.fromArenaOperationString(wrapper.opType),
			wrapper.pos
		)

		val translation = getTranslation(arenaData.arenaId.toLong(), AktivitetKategori.TILTAKSAKTIVITET)
		val message = if (translation != null) getOutputMessage(translation.aktivitetId) else null

		return AktivitetResult(
			arenaData.operationPosition,
			arenaData,
			translation,
			message
		)
	}

	private fun getOutputMessage(id: UUID): KafkaMessageDto<TiltakAktivitet>? {
		var attempts = 0
		while (attempts < 5) {
			val data = outputMessages.firstOrNull { it.payload != null && (it.payload as TiltakAktivitet).id == id }

			if (data != null) {
				outputMessages.remove(data)
				return data
			}

			Thread.sleep(250)
			attempts++
		}

		return null
	}

}
