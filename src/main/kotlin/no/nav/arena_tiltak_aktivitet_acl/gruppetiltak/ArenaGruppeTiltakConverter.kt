package no.nav.arena_tiltak_aktivitet_acl.gruppetiltak

import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.*
import java.time.LocalDate
import java.util.*

object ArenaGruppeTiltakConverter {
	fun toAktivitetStatus(startDato: LocalDate?, sluttDato: LocalDate?, kafkaOperation: Operation): AktivitetStatus {
		val now = LocalDate.now();
		return if (kafkaOperation == Operation.DELETED) AktivitetStatus.AVBRUTT
		else if (startDato == null) AktivitetStatus.PLANLAGT
		else if (startDato.isBefore(now)) AktivitetStatus.PLANLAGT
		else if (sluttDato == null) AktivitetStatus.GJENNOMFORES
		else if (sluttDato.isAfter(now)) AktivitetStatus.GJENNOMFORES
		else AktivitetStatus.FULLFORT
	}
}
