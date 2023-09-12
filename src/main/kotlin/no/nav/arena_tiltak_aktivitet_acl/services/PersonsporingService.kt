package no.nav.arena_tiltak_aktivitet_acl.services

import no.nav.arena_tiltak_aktivitet_acl.repositories.PersonSporingDbo
import no.nav.arena_tiltak_aktivitet_acl.repositories.PersonSporingRepository
import org.springframework.stereotype.Service

@Service
class PersonsporingService (
	val personSporingRepository: PersonSporingRepository
	) {
	fun upsert(personSporingDbo: PersonSporingDbo) = personSporingRepository.upsert(personSporingDbo)
	fun get(personId: Long, gjennomforingId: Long) = personSporingRepository.get(personId, gjennomforingId)
}
