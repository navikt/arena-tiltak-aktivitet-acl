package no.nav.arena_tiltak_aktivitet_acl.domain

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Aktivitetskort
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetskortHeaders
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.processors.AktivitetskortOppfolgingsperiode
import java.time.LocalDateTime
import java.util.UUID

interface InternalDomainObject {
	fun toAktivitetskort(aktivitetId: UUID, erNy: Boolean, operation: Operation): Aktivitetskort
	fun toAktivitetskortHeaders(oppfolgingsperiode: AktivitetskortOppfolgingsperiode): AktivitetskortHeaders
	fun arenaId(): Long
	fun opprettet(): LocalDateTime
}
