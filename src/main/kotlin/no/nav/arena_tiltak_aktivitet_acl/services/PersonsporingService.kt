package no.nav.arena_tiltak_aktivitet_acl.services

import ArenaOrdsProxyClient
import no.nav.arena_tiltak_aktivitet_acl.repositories.PersonSporingDbo
import no.nav.arena_tiltak_aktivitet_acl.repositories.PersonSporingRepository
import org.springframework.stereotype.Service

@Service
class PersonsporingService (
	val personSporingRepository: PersonSporingRepository,
	private val ordsClient: ArenaOrdsProxyClient
	) {
	fun get(arenaPersonId: Long, gjennomforingId: Long): PersonSporingDbo {
		val personSporingDbo = personSporingRepository.get(arenaPersonId, gjennomforingId)
		if (personSporingDbo != null) {
			return personSporingDbo
		}
		val fnr = ordsClient.hentFnr(arenaPersonId) ?: throw IllegalStateException("Expected person with personId=${arenaPersonId} to exist")
		val newDbo = PersonSporingDbo(arenaPersonId, fnr, gjennomforingId)
		personSporingRepository.upsert(newDbo)
		return PersonSporingDbo(arenaPersonId, fnr, gjennomforingId)
	}
}
