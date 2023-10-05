package no.nav.arena_tiltak_aktivitet_acl.processors

import ArenaOrdsProxyClient
import no.nav.arena_tiltak_aktivitet_acl.clients.amt_enhetsregister.EnhetsregisterClient
import no.nav.arena_tiltak_aktivitet_acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.ArenaGjennomforingKafkaMessage
import no.nav.arena_tiltak_aktivitet_acl.exceptions.IgnoredException
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.GjennomforingRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
open class GjennomforingProcessor(
	private val arenaDataRepository: ArenaDataRepository,
	private val gjennomforingRepository: GjennomforingRepository,
	private val ordsClient: ArenaOrdsProxyClient,
	private val enhetsregisterClient: EnhetsregisterClient
) : ArenaMessageProcessor<ArenaGjennomforingKafkaMessage> {

	private val log = LoggerFactory.getLogger(javaClass)

	override fun handleArenaMessage(message: ArenaGjennomforingKafkaMessage) {
		val gjennomforing = message.getData().mapTiltakGjennomforing()

		if (message.operationType == Operation.DELETED) {
			throw IgnoredException("Skal ignorere gjennomforing med operation type DELETE")
		}

		val virksomhetsnummer = gjennomforing.arbgivIdArrangor?.let { ordsClient.hentVirksomhetsnummer(it) }
		val virksomhet = virksomhetsnummer?.let { enhetsregisterClient.hentVirksomhet(virksomhetsnummer) }

		val gjennomforingDbo = gjennomforing.toDbo(virksomhetsnummer, virksomhet?.navn)

		arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(gjennomforing.arenaId))
		gjennomforingRepository.upsert(gjennomforingDbo)
		log.info("Upsert av gjennomføring " +
			"arenaId=${gjennomforing.arenaId} navn=${gjennomforing.lokaltNavn} tiltakkode=${gjennomforing.tiltakKode}")
	}


}
