package no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet

import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.header.internals.RecordHeader
import java.nio.charset.Charset
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
		fun fromKafkaHeaders(kafkaHeaders: Headers) = AktivitetskortHeaders(
			arenaId = kafkaHeaders.lastHeader("eksternReferanseId").value().toString(charset = Charset.defaultCharset()),
			tiltakKode = kafkaHeaders.lastHeader("arenaTiltakskode").value().toString(charset = Charset.defaultCharset()),
			oppfolgingsperiode = UUID.fromString(kafkaHeaders.lastHeader("oppfolgingsperiode")?.toStringNullable()),
			oppfolgingsSluttDato = kafkaHeaders.lastHeader("oppfolgingsperiodeSlutt")?.toStringNullable()
				?.let { ZonedDateTime.parse(it) }
		)
	}
	fun toKafkaHeaders(): List<Header> {
		return listOfNotNull(
			RecordHeader("arenaTiltakskode", this.tiltakKode.toByteArray()),
			RecordHeader("eksternReferanseId", this.arenaId.toByteArray()),
			this.oppfolgingsperiode?.let { RecordHeader("oppfolgingsperiode", it.toString().toByteArray()) } ,
			this.oppfolgingsSluttDato?.let { RecordHeader("oppfolgingsperiodeSlutt", it.toString().toByteArray()) } ,
		)
	}
}


fun Header?.toStringNullable(): String? {
	if (this?.value() == null) return null
	return this.value().toString(charset = Charset.defaultCharset())
}




