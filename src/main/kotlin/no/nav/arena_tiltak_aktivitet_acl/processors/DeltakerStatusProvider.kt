package no.nav.arena_tiltak_aktivitet_acl.processors

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.amt.AmtDeltaker
import java.time.LocalDateTime

interface DeltakerStatusProvider {
	fun getStatus () : AmtDeltaker.Status
	fun getEndretDato () : LocalDateTime?
}
