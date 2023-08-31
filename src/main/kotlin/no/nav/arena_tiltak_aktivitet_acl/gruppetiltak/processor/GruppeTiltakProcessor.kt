package no.nav.arena_tiltak_aktivitet_acl.gruppetiltak.processor

import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Aktivitetskort
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetskortHeaders
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.ArenaGruppeTiltakKafkaMessage
import no.nav.arena_tiltak_aktivitet_acl.exceptions.IgnoredException
import no.nav.arena_tiltak_aktivitet_acl.exceptions.OutOfOrderException
import no.nav.arena_tiltak_aktivitet_acl.gruppetiltak.GruppeTiltak
import no.nav.arena_tiltak_aktivitet_acl.gruppetiltak.kafka.arena.ArenaGruppeTiltakEndretDto
import no.nav.arena_tiltak_aktivitet_acl.processors.AktivitetskortOppfolgingsperiode
import no.nav.arena_tiltak_aktivitet_acl.processors.ArenaMessageProcessor
import no.nav.arena_tiltak_aktivitet_acl.processors.MessageSteps
import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetDbo
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.oppfolgingsPeriode
import no.nav.arena_tiltak_aktivitet_acl.services.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.Month
import java.util.UUID

typealias GruppeMessage = ArenaGruppeTiltakKafkaMessage
@Component
open class GruppeTiltakProcessor(
	private val arenaDataRepository: ArenaDataRepository,
	private val arenaIdTranslationService: TranslationService,
	private val kafkaProducerService: KafkaProducerService,
	private val aktivitetService: AktivitetService,
	private val oppfolgingsperiodeService: OppfolgingsperiodeService
) : ArenaMessageProcessor<ArenaGruppeTiltakKafkaMessage> {
	companion object {
		val AKTIVITETSPLAN_LANSERINGSDATO: LocalDateTime = LocalDateTime.of(2017, Month.DECEMBER, 4, 0,0)
	}
	private val log = LoggerFactory.getLogger(javaClass)

	override fun handleArenaMessage(message: GruppeMessage) {
		val unprocessedMessage = MessageSteps.UncheckedMessage(message)
		if (unprocessedMessage.message.operationType == Operation.DELETED) {
			throw IgnoredException("Skal ignorere gruppetiltak med operation type DELETE")
		}
		if (unprocessedMessage.data.opprettet().isBefore(AKTIVITETSPLAN_LANSERINGSDATO)) {
			throw IgnoredException("Gruppetiltak registrert=${unprocessedMessage.data.opprettet()} opprettet før aktivitetsplan skal ikke håndteres")
		}

		val message = unprocessedMessage
			.sjekkErNesteSomSkalBehandles()
			.enrichWithAtivitetId()
			.enrichWithAktivitetDBO()
			.enrichWithOppfolging()

		arenaDataRepository.upsert(message.message.toUpsertInputWithStatusHandled(message.data.arenaId()))
		aktivitetService.upsert(message.aktivitetskort, message.aktivitetskortHeaders)
		kafkaProducerService.sendTilAktivitetskortTopic(message)
	}

	fun KafkaProducerService.sendTilAktivitetskortTopic(message: MessageSteps.MessageWithOppfolgingsperiode<ArenaGruppeTiltakEndretDto>) = this.sendTilAktivitetskortTopic(
		message.aktivitetskort.id,
		message.aktivitetskort.toKafkaMessage(),
		message.aktivitetskortHeaders
	)

	fun MessageSteps.RawMessage<ArenaGruppeTiltakEndretDto>.enrichWithAtivitetId() =
		this.withAktivitetId(arenaIdTranslationService.hentEllerOpprettAktivitetId(
			this.data.arenaId(), AktivitetKategori.GRUPPEAKTIVITET).id)

	fun MessageSteps.MessageWithId<ArenaGruppeTiltakEndretDto>.enrichWithAktivitetDBO() =
		this.withDBO(aktivitetService.get(this.aktivitetId))

	fun MessageSteps.MessageWithHistory<ArenaGruppeTiltakEndretDto>.enrichWithOppfolging(): MessageSteps.MessageWithOppfolgingsperiode<ArenaGruppeTiltakEndretDto> {
		val data = this.data
		val oppfolgingsperiode = this.aktivitetskortDbo?.oppfolgingsPeriode()
			?: oppfolgingsperiodeService.getOppfolgingsPeriodeOrThrow(this.aktivitetskort,
				data.opprettet(), data.arenaId())
		return this.withOppfolging(oppfolgingsperiode)
	}

	fun MessageSteps.UncheckedMessage<ArenaGruppeTiltakEndretDto>.sjekkErNesteSomSkalBehandles(): MessageSteps.RawMessage<ArenaGruppeTiltakEndretDto> {
		val ingestStatus: IngestStatus? = runCatching {
			arenaDataRepository.get(
				this.message.arenaTableName,
				this.message.operationType,
				this.message.operationPosition
			).ingestStatus
		}.getOrNull()
		val hasUnhandled = arenaDataRepository.hasUnhandledGruppetiltak(this.data.arenaId())
		val isFirstInQueue = ingestStatus == IngestStatus.RETRY || ingestStatus == IngestStatus.FAILED
		if (hasUnhandled && !isFirstInQueue) throw OutOfOrderException("Venter på at tidligere gruppetiltak med id=${this.data.arenaId()} skal bli håndtert")
		return MessageSteps.RawMessage(message = message)
	}

}
