package no.nav.arena_tiltak_aktivitet_acl.gruppetiltak.processor

import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetskortHeaders
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.ArenaGruppeTiltakKafkaMessage
import no.nav.arena_tiltak_aktivitet_acl.exceptions.IgnoredException
import no.nav.arena_tiltak_aktivitet_acl.exceptions.OutOfOrderException
import no.nav.arena_tiltak_aktivitet_acl.gruppetiltak.GruppeTiltak
import no.nav.arena_tiltak_aktivitet_acl.processors.AktivitetskortOppfolgingsperiode
import no.nav.arena_tiltak_aktivitet_acl.processors.ArenaMessageProcessor
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.oppfolgingsPeriode
import no.nav.arena_tiltak_aktivitet_acl.services.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.Month


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

	override fun handleArenaMessage(message: ArenaGruppeTiltakKafkaMessage) {
		val gruppeTiltak = message.getData().toGruppeTiltak()
		if (message.operationType == Operation.DELETED) {
			throw IgnoredException("Skal ignorere gruppetiltak med operation type DELETE")
		}
		if (gruppeTiltak.opprettetTid.isBefore(AKTIVITETSPLAN_LANSERINGSDATO)) {
			throw IgnoredException("Gruppetiltak registrert=${gruppeTiltak.opprettetTid} opprettet før aktivitetsplan skal ikke håndteres")
		}
		sjekkErNesteSomSkalBehandles(message)

		val aktivitetId = arenaIdTranslationService.hentEllerOpprettAktivitetId(gruppeTiltak.arenaAktivitetId, AktivitetKategori.GRUPPEAKTIVITET)

		val aktivitetskortDbo = aktivitetService.get(aktivitetId)
		val erNyAktivitet = aktivitetskortDbo != null
		val aktivitetskort = gruppeTiltak
			.convertToTiltaksaktivitet(
				aktivitetId = aktivitetId,
				nyAktivitet = erNyAktivitet,
				kafkaOperation = message.operationType
			)
		val oppfolgingsperiode = aktivitetskortDbo?.oppfolgingsPeriode()
			?: oppfolgingsperiodeService.getOppfolgingsPeriodeOrThrow(aktivitetskort, gruppeTiltak.opprettetTid, gruppeTiltak.arenaAktivitetId)

		val aktivitetskortHeaders =  gruppeTiltak.toAktivitetkortHeaders(oppfolgingsperiode)
		arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(gruppeTiltak.arenaAktivitetId))
		aktivitetService.upsert(aktivitetskort, aktivitetskortHeaders)
		kafkaProducerService.sendTilAktivitetskortTopic(
			aktivitetskort.id,
			aktivitetskort.toKafkaMessage(),
			aktivitetskortHeaders
		)
	}

	fun sjekkErNesteSomSkalBehandles(message: ArenaGruppeTiltakKafkaMessage) {
		val gruppeTiltak = message.getData().toGruppeTiltak()
		val ingestStatus: IngestStatus? = runCatching {
			arenaDataRepository.get(
				message.arenaTableName,
				message.operationType,
				message.operationPosition
			).ingestStatus
		}.getOrNull()
		val hasUnhandled = arenaDataRepository.hasUnhandledGruppetiltak(gruppeTiltak.arenaAktivitetId)
		val isFirstInQueue = ingestStatus == IngestStatus.RETRY || ingestStatus == IngestStatus.FAILED
		if (hasUnhandled && !isFirstInQueue) throw OutOfOrderException("Venter på at tidligere gruppetiltak med id=${gruppeTiltak.arenaAktivitetId} skal bli håndtert")
	}

}

fun GruppeTiltak.toAktivitetkortHeaders(oppfolgingsperiode: AktivitetskortOppfolgingsperiode) = AktivitetskortHeaders(
	arenaId = this.getArenaIdWithPrefix(),
	tiltakKode = this.aktivitetstype,
	oppfolgingsperiode = oppfolgingsperiode.id,
	oppfolgingsSluttDato = oppfolgingsperiode.oppfolgingsSluttDato,
)
