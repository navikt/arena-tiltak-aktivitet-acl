package no.nav.arena_tiltak_aktivitet_acl.integration.executors

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.gjennomforing.GjennomforingCommand
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.gjennomforing.GjennomforingResult
import no.nav.arena_tiltak_aktivitet_acl.repositories.TranslationRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.GjennomforingRepository
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
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
		return sendAndCheck(
			command.toArenaKafkaMessageDto(incrementAndGetPosition()),
			command.key
		)
	}

	private fun sendAndCheck(arenaWrapper: ArenaKafkaMessageDto, key: String): GjennomforingResult {
		sendKafkaMessage(topic, objectMapper.writeValueAsString(arenaWrapper), key)
		return getResults(arenaWrapper)
	}

	private fun getResults(arenaWrapper: ArenaKafkaMessageDto): GjennomforingResult {
		val arenaData = getArenaData(
			ArenaTableName.GJENNOMFORING,
			Operation.fromArenaOperationString(arenaWrapper.opType),
			arenaWrapper.pos
		)

		val output = gjennomforingRepository.get(arenaData.arenaId.toLong())
		return GjennomforingResult(arenaWrapper.pos, arenaData, output)
	}

}

