package no.nav.arena_tiltak_aktivitet_acl.processors

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaKafkaMessage

interface ArenaMessageProcessor<M : ArenaKafkaMessage<*>> {

	fun handleArenaMessage(message: M)

}
