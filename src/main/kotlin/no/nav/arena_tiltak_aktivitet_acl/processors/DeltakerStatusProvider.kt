package no.nav.arena_tiltak_aktivitet_acl.processors

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Aktivitet
import java.time.LocalDateTime

interface DeltakerStatusProvider {
	fun getStatus () : Aktivitet.Status
	fun getEndretDato () : LocalDateTime?
}
