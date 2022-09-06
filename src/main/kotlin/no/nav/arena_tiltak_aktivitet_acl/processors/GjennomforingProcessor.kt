package no.nav.arena_tiltak_aktivitet_acl.processors

import ArenaOrdsProxyClient
import no.nav.arena_tiltak_aktivitet_acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaGjennomforingKafkaMessage
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.GjennomforingRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
open class GjennomforingProcessor(
	private val arenaDataRepository: ArenaDataRepository,
	private val gjennomforingRepository: GjennomforingRepository,
	private val ordsClient: ArenaOrdsProxyClient,
) : ArenaMessageProcessor<ArenaGjennomforingKafkaMessage> {

	private val log = LoggerFactory.getLogger(javaClass)

	override fun handleArenaMessage(message: ArenaGjennomforingKafkaMessage) {
		val gjennomforing = message.getData().mapTiltakGjennomforing()

		val virksomhetsnummer = gjennomforing.arbgivIdArrangor?.let { ordsClient.hentVirksomhetsnummer(it) }
		val gjennomforingDbo = gjennomforing.toDbo(virksomhetsnummer, "") //TODO: hente arrangørnavn fra enhetsregisteret

		arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(gjennomforing.arenaId))
		gjennomforingRepository.upsert(gjennomforingDbo)
		log.info("Melding for gjennomføring " +
			"arenaId=${gjennomforing.arenaId} er håndtert")
	}


}
