package no.nav.arena_tiltak_aktivitet_acl.processors

import no.nav.arena_tiltak_aktivitet_acl.clients.oppfolging.Oppfolgingsperiode
import no.nav.arena_tiltak_aktivitet_acl.domain.db.DeltakerAktivitetMappingDbo
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetKategori
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.AktivitetskortHeaders
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Tiltak
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.ArenaDeltakerKafkaMessage
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.TiltakDeltaker
import no.nav.arena_tiltak_aktivitet_acl.exceptions.DependencyNotIngestedException
import no.nav.arena_tiltak_aktivitet_acl.exceptions.IgnoredException
import no.nav.arena_tiltak_aktivitet_acl.exceptions.OppfolgingsperiodeNotFoundException
import no.nav.arena_tiltak_aktivitet_acl.exceptions.OutOfOrderException
import no.nav.arena_tiltak_aktivitet_acl.processors.converters.ArenaDeltakerConverter
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.DeltakerAktivitetMappingRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.GjennomforingRepository
import no.nav.arena_tiltak_aktivitet_acl.services.*
import no.nav.arena_tiltak_aktivitet_acl.services.OppfolgingsperiodeService.Companion.defaultSlakk
import no.nav.arena_tiltak_aktivitet_acl.services.OppfolgingsperiodeService.Companion.tidspunktTidligereEnnRettFoerStartDato
import no.nav.arena_tiltak_aktivitet_acl.utils.SecureLog.secureLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.util.*

@Component
open class DeltakerProcessor(
	private val arenaDataRepository: ArenaDataRepository,
	private val arenaIdTranslationService: TranslationService,
	private val kafkaProducerService: KafkaProducerService,
	private val gjennomforingRepository: GjennomforingRepository,
	private val aktivitetService: AktivitetService,
	private val tiltakService: TiltakService,
	private val personsporingService: PersonsporingService,
	private val oppfolgingsperiodeService: OppfolgingsperiodeService,
	private val deltakerAktivitetMappingRepository: DeltakerAktivitetMappingRepository
) : ArenaMessageProcessor<ArenaDeltakerKafkaMessage> {

	companion object {
		val AKTIVITETSPLAN_LANSERINGSDATO: LocalDateTime = LocalDateTime.of(2017, Month.DECEMBER, 4, 0,0)
	}

	private val log = LoggerFactory.getLogger(javaClass)

	override fun handleArenaMessage(message: ArenaDeltakerKafkaMessage) {
		val arenaDeltaker = message.getData()
		val arenaGjennomforingId = arenaDeltaker.TILTAKGJENNOMFORING_ID
		val deltaker = arenaDeltaker.mapTiltakDeltaker()

		if (message.operationType == Operation.DELETED) {
			throw IgnoredException("Skal ignorere deltakelse med operation type DELETE")
		}
		if (deltaker.regDato.isBefore(AKTIVITETSPLAN_LANSERINGSDATO)) {
			throw IgnoredException("Deltakeren registrert=${deltaker.regDato} opprettet før aktivitetsplan skal ikke håndteres")
		}
		val ingestStatus: IngestStatus? = runCatching {
			arenaDataRepository.get(
				message.arenaTableName,
				message.operationType,
				message.operationPosition
			).ingestStatus
		}.getOrNull()

		val hasUnhandled = arenaDataRepository.hasUnhandledDeltakelse(arenaDeltaker.TILTAKDELTAKER_ID)
		val isFirstInQueue = ingestStatus == IngestStatus.RETRY || ingestStatus == IngestStatus.FAILED
		if (hasUnhandled && !isFirstInQueue) throw OutOfOrderException("Venter på at tidligere deltakelse med id=${arenaDeltaker.TILTAKDELTAKER_ID} skal bli håndtert")

		val gjennomforing = gjennomforingRepository.get(arenaGjennomforingId)
			?: throw DependencyNotIngestedException("Venter på at gjennomføring med id=$arenaGjennomforingId skal bli håndtert")

		val tiltak = tiltakService.getByKode(gjennomforing.tiltakKode)
			?: throw DependencyNotIngestedException("Venter på at tiltak med id=${gjennomforing.tiltakKode} skal bli håndtert")

		if (skalIgnoreres(arenaDeltaker.DELTAKERSTATUSKODE, tiltak.administrasjonskode)) {
			throw IgnoredException("Deltakeren har status=${arenaDeltaker.DELTAKERSTATUSKODE} og administrasjonskode=${tiltak.administrasjonskode} som ikke skal håndteres")
		}
		val deltakerAktivitetMapping = deltakerAktivitetMappingRepository.get(deltaker.tiltakdeltakerId)
		val oppfolgingsperioder = deltakerAktivitetMapping.map { mapping -> mapping.oppfolgingsperiodeUuid }
		val personIdent = personsporingService.get(deltaker.personId, arenaGjennomforingId).fodselsnummer

		val erNyDeltakelse = (oppfolgingsperioder.isEmpty())

		/*
		 Hvis oppfølgingsperiode ikke finnes,
		 hopper vi ut her, enten med retry eller ignored, siden handleOppfolgingsperiodeNull kaster exception alltid.
		 Dette er viktig for å ikke opprette ny aktivitetsid før vi faktisk lagrer et aktivitetskort.
		*/
		val oppfolgingsperiodePaaEndringsTidspunkt = getOppfolgingsPeriodeOrThrow(deltaker, personIdent, deltaker.modDato ?: deltaker.regDato, deltaker.tiltakdeltakerId)


		val (nyAktivitet, faktiskAktivitetsId) =
			// Det finnes allerede minst ett aktivitetskort for denne deltakelsen
			if (!erNyDeltakelse) {
				if (!oppfolgingsperioder.contains(oppfolgingsperiodePaaEndringsTidspunkt!!.uuid)) {
				// Har har det kommet en endring på kortet under en annen oppfølgingsperiode enn den opprinnelige oppfølgingsperioden. Vi oppretter et helt nytt aktivitetskort.
					val gjeldendeAktivitetsId = arenaIdTranslationService.hentAktivitetIdForArenaId(deltaker.tiltakdeltakerId, AktivitetKategori.TILTAKSAKTIVITET)!!
					val nyAktivitetsId = UUID.randomUUID()
					secureLog.info("Endring på deltakelse ${deltaker.tiltakdeltakerId} fra gjeldende aktivitetsid ${gjeldendeAktivitetsId} til ny aktivitetsid ${nyAktivitetsId} og oppfølgingsperiode ${oppfolgingsperiodePaaEndringsTidspunkt}. " +
						"Oppretter nytt aktivitetskort for personIdent $personIdent og endrer eksisterende translation entry")
					deltakerAktivitetMappingRepository.insert(DeltakerAktivitetMappingDbo(deltakerId = deltaker.tiltakdeltakerId, aktivitetId = nyAktivitetsId, oppfolgingsperiodeUuid = oppfolgingsperiodePaaEndringsTidspunkt.uuid))
					arenaIdTranslationService.oppdaterAktivitetId( gjeldendeAktivitetsId, nyAktivitetsId)
					// Vi setter nyAktivitet til false, selv om vi oppretter ny aktivitet, slik at mod-dato blir brukt som endretTidspunkt på aktivitetskortet
					false to nyAktivitetsId
				} else {
					val eksisterendeAktivitetsId = deltakerAktivitetMapping.filter { it.oppfolgingsperiodeUuid == oppfolgingsperiodePaaEndringsTidspunkt.uuid }.map {it.aktivitetId}.first()
					// oppfølgingsperiode har ikke endret seg (happy case)
					false to eksisterendeAktivitetsId
				}
			} else { // Ny aktivitet
				val nyAktivitetsId = arenaIdTranslationService.opprettAktivitetsId(deltaker.tiltakdeltakerId, AktivitetKategori.TILTAKSAKTIVITET)
				deltakerAktivitetMappingRepository.insert(DeltakerAktivitetMappingDbo(deltakerId = deltaker.tiltakdeltakerId, aktivitetId = nyAktivitetsId, oppfolgingsperiodeUuid = oppfolgingsperiodePaaEndringsTidspunkt!!.uuid))
				true to nyAktivitetsId
			}


		val fallbackGjennomforingNavn = "Ukjent navn"

		val aktivitet = ArenaDeltakerConverter
			.convertToTiltaksaktivitet(
				deltaker = deltaker,
				aktivitetId = faktiskAktivitetsId,
				personIdent = personIdent,
				arrangorNavn = gjennomforing.arrangorNavn,
				gjennomforingNavn = gjennomforing.navn ?: fallbackGjennomforingNavn,
				tiltak = tiltak,
				erNyAktivitet = nyAktivitet,
			)
		val aktivitetskortHeaders = AktivitetskortHeaders(
			arenaId = KafkaProducerService.TILTAK_ID_PREFIX + deltaker.tiltakdeltakerId.toString(),
			tiltakKode = tiltak.kode,
			oppfolgingsperiode = oppfolgingsperiodePaaEndringsTidspunkt.uuid,
			oppfolgingsSluttDato = oppfolgingsperiodePaaEndringsTidspunkt.sluttDato
		)
		val outgoingMessage = aktivitet.toKafkaMessage()
		kafkaProducerService.sendTilAktivitetskortTopic(
			aktivitet.id,
			outgoingMessage,
			aktivitetskortHeaders
		)
		secureLog.info("Melding for aktivitetskort id=$faktiskAktivitetsId arenaId=${deltaker.tiltakdeltakerId} personId=${deltaker.personId} fnr=$personIdent er sendt")
		log.info("Melding id=${outgoingMessage.messageId} aktivitetskort id=$faktiskAktivitetsId  arenaId=${deltaker.tiltakdeltakerId} type=${outgoingMessage.actionType} er sendt")
		aktivitetService.upsert(aktivitet, aktivitetskortHeaders)
		arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(deltaker.tiltakdeltakerId))
	}

	//	Alle tiltaksaktiviteter hentes med unntak for tiltak av
	//	administrasjonstypene Institusjonelt tiltak (INST) og Individuelt tiltak (IND) som har deltakerstatus Aktuell (AKTUELL).
	private fun skalIgnoreres(arenaDeltakerStatusKode: String, administrasjonskode: Tiltak.Administrasjonskode): Boolean {
		return arenaDeltakerStatusKode == "AKTUELL"
			&& administrasjonskode in listOf(Tiltak.Administrasjonskode.IND, Tiltak.Administrasjonskode.INST)
	}

	private fun handleOppfolgingsperiodeNull(deltaker: TiltakDeltaker, personIdent: String, tidspunkt: LocalDateTime, tiltakDeltakerId: Long) {
		secureLog.info("Fant ikke oppfølgingsperiode for personIdent=$personIdent")
		val aktivitetStatus = ArenaDeltakerConverter.toAktivitetStatus(deltaker.deltakerStatusKode)
		val erFerdig = deltaker.datoTil?.isBefore(LocalDate.now()) ?: false
		when {
			aktivitetStatus.erAvsluttet() || erFerdig ->
				throw IgnoredException("Avsluttet deltakelse og ingen oppfølgingsperiode, id=${tiltakDeltakerId}")
			tidspunktTidligereEnnRettFoerStartDato(tidspunkt, LocalDateTime.now(), defaultSlakk) ->
				throw IgnoredException("Opprettet for mer enn $defaultSlakk siden og ingen oppfølgingsperiode, id=${tiltakDeltakerId}")
			else -> throw OppfolgingsperiodeNotFoundException("Deltakelse endret tidspunkt=${tidspunkt}, Finner ingen passende oppfølgingsperiode, id=${tiltakDeltakerId}")
		}
	}

	private fun getOppfolgingsPeriodeOrThrow(deltaker: TiltakDeltaker, personIdent: String, tidspunkt: LocalDateTime, tiltakDeltakerId: Long): Oppfolgingsperiode? {
		val oppfolgingsperiode = oppfolgingsperiodeService.finnOppfolgingsperiode(personIdent, tidspunkt)
		return if (oppfolgingsperiode == null) {
			handleOppfolgingsperiodeNull(deltaker, personIdent, tidspunkt, tiltakDeltakerId) // throws always
			null
		} else oppfolgingsperiode
	}
}




