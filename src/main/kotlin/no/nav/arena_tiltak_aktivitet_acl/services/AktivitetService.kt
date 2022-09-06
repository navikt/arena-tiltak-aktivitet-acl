package no.nav.arena_tiltak_aktivitet_acl.services

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.TiltakAktivitet
import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetRepository
import no.nav.arena_tiltak_aktivitet_acl.utils.ObjectMapperFactory
import org.springframework.stereotype.Service

@Service
class AktivitetService(
	val aktivitetRepository: AktivitetRepository
) {
	private val objectMapper = ObjectMapperFactory.get()

	fun insert(aktivitet: TiltakAktivitet) {
		val data = objectMapper.writeValueAsString(aktivitet)
		aktivitetRepository.insert(aktivitet.toDbo(data))
	}
}
