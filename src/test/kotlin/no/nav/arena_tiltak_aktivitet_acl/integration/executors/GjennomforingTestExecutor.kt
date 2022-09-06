package no.nav.arena_tiltak_aktivitet_acl.integration.executors

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.gjennomforing.GjennomforingCommand
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.gjennomforing.GjennomforingResult
import no.nav.arena_tiltak_aktivitet_acl.repositories.TranslationRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.GjennomforingRepository
import no.nav.arena_tiltak_aktivitet_acl.utils.ARENA_GJENNOMFORING_TABLE_NAME
import no.nav.common.kafka.producer.KafkaProducerClientImpl

class GjennomforingTestExecutor(
	kafkaProducer: KafkaProducerClientImpl<String, String>,
	arenaDataRepository: ArenaDataRepository,
	val gjennomforingRepository: GjennomforingRepository,
	translationRepository: TranslationRepository
) : TestExecutor(
	kafkaProducer = kafkaProducer,
	arenaDataRepository = arenaDataRepository,
	translationRepository = translationRepository
) {

	private val topic = "gjennomforing"

	fun execute(command: GjennomforingCommand): GjennomforingResult {
		return command.execute(incrementAndGetPosition()) { sendAndCheck(it) }
	}

	private fun sendAndCheck(arenaWrapper: ArenaKafkaMessageDto): GjennomforingResult {
		sendKafkaMessage(topic, objectMapper.writeValueAsString(arenaWrapper))
		return getResults(arenaWrapper)
	}

	private fun getResults(arenaWrapper: ArenaKafkaMessageDto): GjennomforingResult {
		val arenaData = getArenaData(
			ARENA_GJENNOMFORING_TABLE_NAME,
			Operation.fromArenaOperationString(arenaWrapper.opType),
			arenaWrapper.pos
		)

		val output = gjennomforingRepository.get(arenaData.arenaId.toLong())
		return GjennomforingResult(arenaWrapper.pos, arenaData, output)
	}

}

