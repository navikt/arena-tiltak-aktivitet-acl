package no.nav.arena_tiltak_aktivitet_acl.integration.executors

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.OperationPos
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.tiltak.TiltakCommand
import no.nav.arena_tiltak_aktivitet_acl.integration.commands.tiltak.TiltakResult
import no.nav.arena_tiltak_aktivitet_acl.integration.utils.Retry.nullableAsyncRetryHandler
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.TiltakRepository
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import org.junit.jupiter.api.fail

class TiltakTestExecutor(
	kafkaProducer: KafkaProducerClientImpl<String, String>,
	arenaDataRepository: ArenaDataRepository,
	private val tiltakRepository: TiltakRepository
) : TestExecutor(
	kafkaProducer = kafkaProducer,
	arenaDataRepository = arenaDataRepository,
) {

	private val topic = "tiltak"

	fun execute(command: TiltakCommand): TiltakResult {
		return sendKafkaMessageOgVentPaAck(
			command.toArenaKafkaMessageDto(incrementAndGetPosition()),
			command.tiltaksKode
		)
	}

	private fun sendKafkaMessageOgVentPaAck(arenaWrapper: ArenaKafkaMessageDto, kode: String): TiltakResult {
		sendKafkaMessage(topic, objectMapper.writeValueAsString(arenaWrapper), kode)
		val data = pollArenaData(
			ArenaTableName.TILTAK,
			Operation.fromArenaOperationString(arenaWrapper.opType),
			OperationPos.of(arenaWrapper.pos)
		)
		val storedTiltak = nullableAsyncRetryHandler("get tiltak by kode: $kode") { tiltakRepository.getByKode(kode) }
			?: fail("Forventet at tiltak med kode $kode ligger i tiltak databasen.")
		return TiltakResult(
			arenaDataDbo = data,
			tiltak = storedTiltak
		)
	}
}
