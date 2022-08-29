package no.nav.arena_tiltak_aktivitet_acl.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.ArenaDeltakerKafkaMessage
import org.springframework.stereotype.Component

@Component
class DeltakerMetricHandler(
	private val registry: MeterRegistry
) {

	fun publishMetrics(message: ArenaDeltakerKafkaMessage) {
		if (message.operationType == Operation.CREATED) {
			registry.counter("amt.arena-acl.deltaker.ny").increment()
		} else if (message.operationType == Operation.MODIFIED) {
			val before = message.before
			val after = message.after

			if (before?.DATO_FRA != after?.DATO_FRA) {
				registry.counter(
					"amt.arena-acl.deltaker.oppdatering",
					listOf(Tag.of("field", "startDato"))
				).increment()
			}

			if (before?.DATO_TIL != after?.DATO_TIL) {
				registry.counter(
					"amt.arena-acl.deltaker.oppdatering",
					listOf(Tag.of("field", "sluttDato"))
				).increment()
			}
		}
	}
}
