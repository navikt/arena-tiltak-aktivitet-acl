package no.nav.arena_tiltak_aktivitet_acl.processors.converters

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.*
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.TiltakDeltaker
import java.lang.IllegalArgumentException
import java.util.*

object ArenaDeltakerConverter {
	val JOBBKLUBB = "JOBBKLUBB"
	val AMO = "AMO"
	val GRUPPEAMO = "GRUPPEAMO"
	val ENKELAMO = "ENKELAMO"

	fun toAktivitetStatus(status: String): AktivitetStatus {
		return when (status) {
			"AKTUELL" -> AktivitetStatus.PLANLAGT
			"INFOMOETE" -> AktivitetStatus.PLANLAGT
			"JATAKK" -> AktivitetStatus.PLANLAGT
			"TILBUD" -> AktivitetStatus.PLANLAGT
			"VENTELISTE" -> AktivitetStatus.PLANLAGT
			"FULLF" -> AktivitetStatus.FULLFORT
			"GJENN" -> AktivitetStatus.GJENNOMFORES
			"GJENN_AVB" -> AktivitetStatus.AVBRUTT
			"GJENN_AVL" -> AktivitetStatus.AVBRUTT
			"IKKAKTUELL" -> AktivitetStatus.AVBRUTT
			"AVSLAG" -> AktivitetStatus.AVBRUTT
			"DELAVB" -> AktivitetStatus.AVBRUTT
			"IKKEM" -> AktivitetStatus.AVBRUTT
			"NEITAKK" -> AktivitetStatus.AVBRUTT
			else -> throw IllegalArgumentException("Ugyldig arenastatus $status kan ikke konverteres")
		}
	}

	fun toStatusAarsak(status: String): StatusDto.AarsakType? {
		return when (status) {
			"AKTUELL" -> StatusDto.AarsakType.SOKT_INN
			"INFOMOETE" -> StatusDto.AarsakType.INFOMOETE
			"JATAKK" -> StatusDto.AarsakType.TAKKET_JA
			"TILBUD" -> StatusDto.AarsakType.FATT_PLASS
			"VENTELISTE" -> StatusDto.AarsakType.VENTELISTE
			"IKKAKTUELL" -> StatusDto.AarsakType.IKKE_AKTUELL
			"AVSLAG" -> StatusDto.AarsakType.AVSLAG
			"IKKEM" -> StatusDto.AarsakType.IKKE_MOETT
			"NEITAKK" -> StatusDto.AarsakType.TAKKET_NEI
			else -> null
		}

	}

	fun toTittel(gjennomforingNavn: String, tiltakKode: String): String {
		val prefix = when (tiltakKode) {
			AMO -> "AMO-kurs: "
			GRUPPEAMO -> "Gruppe AMO: "
			ENKELAMO -> "Enkeltplass AMO: "
			else -> ""
		}
		return "$prefix$gjennomforingNavn"

	}

	fun convertToAktivitet(
		deltaker: TiltakDeltaker,
		aktivitetId: UUID,
		personIdent: String,
		arrangorNavn: String?,
		gjennomforingNavn: String,
		tiltak: Tiltak
	): TiltakAktivitet {
		val status = toAktivitetStatus(deltaker.deltakerStatusKode)
		val aarsak = toStatusAarsak(deltaker.deltakerStatusKode)

		return TiltakAktivitet(
			id = aktivitetId,
			personIdent = personIdent,
			tittel = toTittel(gjennomforingNavn, tiltak.kode),
			status = StatusDto(
				status,
				aarsak
			),
			startDato = deltaker.datoFra,
			sluttDato = deltaker.datoTil,
			arrangorNavn = arrangorNavn,
			deltakelseProsent = deltaker.prosentDeltid,
			dagerPerUke = deltaker.dagerPerUke,
			beskrivelse = if (tiltak.kode.equals(JOBBKLUBB)) gjennomforingNavn else null,
			registrertDato = deltaker.regDato,
			statusEndretDato = deltaker.datoStatusendring,
			tiltak = TiltakDto(
				navn = tiltak.navn,
				kode = tiltak.kode
			)
		)
	}
}
