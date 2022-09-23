package no.nav.arena_tiltak_aktivitet_acl.services

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.TiltakAktivitet
import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetRepository
import org.springframework.stereotype.Service

@Service
class AktivitetService(
	val aktivitetRepository: AktivitetRepository
) {
	fun upsert(aktivitet: TiltakAktivitet) = aktivitetRepository.upsert(aktivitet.toDbo())
}
