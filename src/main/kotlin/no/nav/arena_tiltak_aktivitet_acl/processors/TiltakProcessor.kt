package no.nav.arena_tiltak_aktivitet_acl.processors

import io.micrometer.core.annotation.Timed
import no.nav.arena_tiltak_aktivitet_acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaTiltakKafkaMessage
import no.nav.arena_tiltak_aktivitet_acl.exceptions.IgnoredException
import no.nav.arena_tiltak_aktivitet_acl.exceptions.OperationNotImplementedException
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.services.TiltakService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
open class TiltakProcessor(
	private val arenaDataRepository: ArenaDataRepository,
	private val tiltakService: TiltakService,
) : ArenaMessageProcessor<ArenaTiltakKafkaMessage> {

	private val log = LoggerFactory.getLogger(javaClass)

	@Timed(value="tiltakProcessor")
	override fun handleArenaMessage(message: ArenaTiltakKafkaMessage) {
		val data = message.getData()

		if (message.operationType == Operation.DELETED) {
			throw IgnoredException("Skal ignorere tiltak med operation type DELETE")
		}

		val id = UUID.randomUUID()
		val kode = data.TILTAKSKODE
		val navn = data.TILTAKSNAVN

		tiltakService.upsert(
			id = id,
			kode = data.TILTAKSKODE,
			navn = navn,
			administrasjonskode = data.ADMINISTRASJONKODE
		)

		arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(kode))

		log.info("Upsert av tiltak id=$id kode=$kode navn=$navn")
	}

}
