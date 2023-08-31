package no.nav.arena_tiltak_aktivitet_acl.processors

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Aktivitetskort
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaKafkaMessage
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ToDomainAble
import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetDbo
import java.util.*

sealed class MessageSteps<ArenaMessageType: ToDomainAble>(val message: ArenaKafkaMessage<ArenaMessageType>) {
	val data = message.getData().toInternalDataObject()

	class UncheckedMessage<ArenaMessageType: ToDomainAble>(
		message: ArenaKafkaMessage<ArenaMessageType>)
	: MessageSteps<ArenaMessageType>(message)

	class RawMessage<ArenaMessageType: ToDomainAble>(
		message: ArenaKafkaMessage<ArenaMessageType>)
	: MessageSteps<ArenaMessageType>(message) {
		fun withAktivitetId(id: UUID) = MessageWithId(id, message)
	}

	class MessageWithId<ArenaMessageType: ToDomainAble>(
		val aktivitetId: UUID,
		message: ArenaKafkaMessage<ArenaMessageType>)
	: MessageSteps<ArenaMessageType>(message) {
		fun withDBO(aktivitetskortDbo: AktivitetDbo?) = MessageWithHistory(aktivitetId, message, aktivitetskortDbo)
	}

	class MessageWithHistory<ArenaMessageType: ToDomainAble>(
		val aktivitetId: UUID,
		message: ArenaKafkaMessage<ArenaMessageType>,
		val aktivitetskortDbo: AktivitetDbo?):
	MessageSteps<ArenaMessageType>(message) {
		val erNy = aktivitetskortDbo == null
		val aktivitetskort = this.data.toAktivitetskort(
			aktivitetId = aktivitetId,
			erNy = erNy,
			operation = message.operationType)

		fun withOppfolging(oppfolgingsperiode: AktivitetskortOppfolgingsperiode) = MessageWithOppfolgingsperiode(
			aktivitetId, message, oppfolgingsperiode, aktivitetskort)
	}

	class MessageWithOppfolgingsperiode<ArenaMessageType: ToDomainAble>(
		val aktivitetId: UUID, message: ArenaKafkaMessage<ArenaMessageType>,
		val oppfolgingsperiode: AktivitetskortOppfolgingsperiode,
		val aktivitetskort: Aktivitetskort
	): MessageSteps<ArenaMessageType>(message) {
		val aktivitetskortHeaders = data.toAktivitetskortHeaders(oppfolgingsperiode)
	}
}
