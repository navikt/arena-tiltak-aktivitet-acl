package no.nav.arena_tiltak_aktivitet_acl.services

import no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging.AvsluttetOppfolgingsperiode
import no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging.Oppfolgingsperiode
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Aktivitetskort
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetskortHeaders
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AktivitetService(
	val aktivitetRepository: AktivitetRepository
) {
	fun upsert(aktivitet: Aktivitetskort, headers: AktivitetskortHeaders) = aktivitetRepository.upsert(aktivitet.toDbo(headers))
	fun get(aktivitetId: UUID) = aktivitetRepository.getAktivitet(aktivitetId)
	fun getAllBy(aktivitetId: DeltakelseId, aktivitetsKategori: AktivitetKategori) =
		aktivitetRepository.getAllBy(aktivitetId, aktivitetsKategori)

	fun closeClosedPerioder(deltakelseId: DeltakelseId, aktivitetKategori: AktivitetKategori, oppfolgingsperioder: List<Oppfolgingsperiode>) {
		val avsluttedePerioder = oppfolgingsperioder
			.mapNotNull {
				it.sluttDato
					?.let { slutt -> AvsluttetOppfolgingsperiode(it.uuid, it.startDato, slutt) }
			}
		aktivitetRepository.closeClosedPerioder(deltakelseId, aktivitetKategori, avsluttedePerioder)
	}
}
