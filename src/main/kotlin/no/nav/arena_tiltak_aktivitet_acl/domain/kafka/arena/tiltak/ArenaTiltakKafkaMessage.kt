package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaKafkaMessage
import no.nav.arena_tiltak_aktivitet_acl.gruppetiltak.kafka.arena.ArenaGruppeTiltakEndretDto

typealias ArenaTiltakKafkaMessage = ArenaKafkaMessage<ArenaTiltak>

typealias ArenaGjennomforingKafkaMessage = ArenaKafkaMessage<ArenaGjennomforingDto>

typealias ArenaDeltakerKafkaMessage = ArenaKafkaMessage<ArenaDeltaker>

typealias ArenaGruppeTiltakKafkaMessage = ArenaKafkaMessage<ArenaGruppeTiltakEndretDto>
