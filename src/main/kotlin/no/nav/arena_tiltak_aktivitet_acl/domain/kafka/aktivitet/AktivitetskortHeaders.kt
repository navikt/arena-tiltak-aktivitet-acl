package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet

import org.apache.kafka.common.header.Headers
import java.time.ZonedDateTime
import java.util.*

data class AktivitetskortHeaders(
	val arenaId: String,
	val tiltakKode: String,
	val oppfolgingsperiode: UUID?,
	val oppfolgingsSluttDato: ZonedDateTime?,
	)
{
	companion object {
		fun fromKafkaHeaders( kafkaHeaders: Headers) = AktivitetskortHeaders(
			arenaId = kafkaHeaders.lastHeader("").value().toString(),
			tiltakKode = kafkaHeaders.lastHeader("").value().toString(),
			oppfolgingsperiode = UUID.fromString(kafkaHeaders.lastHeader("").value().toString()),
			oppfolgingsSluttDato = ZonedDateTime.parse(kafkaHeaders.lastHeader("").value().toString())
		)

	}
}





