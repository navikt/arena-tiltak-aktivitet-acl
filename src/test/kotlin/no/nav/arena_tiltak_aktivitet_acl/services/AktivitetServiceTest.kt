package no.nav.arena_tiltak_aktivitet_acl.services

import io.kotest.common.runBlocking
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.*
import no.nav.arena_tiltak_aktivitet_acl.database.SingletonPostgresContainer
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.*
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.DeltakelseId
import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetDbo
import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.AktivitetskortIdRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.DeltakelseLockRepository
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class AktivitetServiceTest : StringSpec({
	val log = LoggerFactory.getLogger(javaClass)

	val dataSource = SingletonPostgresContainer.getDataSource()
	val template = NamedParameterJdbcTemplate(dataSource)
	val aktivitetRepository = AktivitetRepository(template)

	fun slowAktivitetRepository(): AktivitetRepository {
		val mock = mockk<AktivitetRepository>()
		val isSlow = AtomicBoolean(true)

		every { mock.upsert(any()) } answers {
			// Make sure first call is slow
			val isFirst = isSlow.getAndSet(false)
			val aktivitet = firstArg<AktivitetDbo>()
			log.info("isFirst $isFirst arenaId: ${aktivitet.arenaId} ${aktivitet}")
			if (isFirst) {
				runBlocking { delay(100) }
				aktivitetRepository.upsert(aktivitet)
			} else {
				aktivitetRepository.upsert(aktivitet)
			}
		}
		return mock
	}
	val aktivitetskortIdRepository = AktivitetskortIdRepository(template)
	val deltakerLockRepository = DeltakelseLockRepository(template)

	val aktivitetskort = Aktivitetskort(
		id = UUID.randomUUID(),
		personIdent = "01234567890",
		tittel = "Tittel",
		aktivitetStatus = AktivitetStatus.GJENNOMFORES,
		etiketter = emptyList(),
		startDato = null,
		sluttDato = null,
		beskrivelse = null,
		endretAv = Ident("IdentType", "ident"),
		endretTidspunkt = LocalDateTime.now(),
		avtaltMedNav = true,
		detaljer = emptyList()
	)

	fun headers(deltakelseId: DeltakelseId): AktivitetskortHeaders {
		return AktivitetskortHeaders(
			"${AktivitetKategori.TILTAKSAKTIVITET.prefix}${deltakelseId.value}",
			"ASDAS",
			UUID.randomUUID(),
			null
		)
	}
	fun aktivitetskort(): Aktivitetskort {
		return aktivitetskort
			.copy(id = UUID.randomUUID())
	}
	fun firstSlowAktivitetsService() = AktivitetService(
		slowAktivitetRepository(),
		aktivitetskortIdRepository,
		deltakerLockRepository
	)

	"skal blokkere prossessring på samme deltakelse-id" {
		val firstSlowAktivitetsService = firstSlowAktivitetsService()
		val deltakelseId = DeltakelseId(1123)
		val aktivitetskort = aktivitetskort()
		val headers = headers(deltakelseId)

		val startOrder = mutableListOf<Long>()
		val excutionOrder = mutableListOf<Long>()
		val first =  async(Dispatchers.IO) {
			startOrder.add(1)
			firstSlowAktivitetsService.upsert(
				aktivitetskort,
				headers,
				deltakelseId,
			)
			excutionOrder.add(1)
		}
		val second = async(Dispatchers.IO) {
			delay(10)
			startOrder.add(2)
			firstSlowAktivitetsService.upsert(
				aktivitetskort,
				headers.copy(oppfolgingsSluttDato = ZonedDateTime.now()),
				deltakelseId,
			)
			excutionOrder.add(2)
		}
		listOf(first, second).awaitAll()
		startOrder shouldBe listOf(1,2)
		excutionOrder shouldBe listOf(1,2)
	}

	"skal ikke blokke andre deltakelse-id-er" {
		val firstSlowAktivitetsService = firstSlowAktivitetsService()
		val deltakelseId = DeltakelseId(2123)
		val deltakelseId2 = DeltakelseId(2321)

		val excutionOrder = mutableListOf<Long>()
		val first =  async(Dispatchers.IO) {
			firstSlowAktivitetsService.upsert(
				aktivitetskort(),
				headers(deltakelseId),
				deltakelseId,
			)
			excutionOrder.add(deltakelseId.value)
		}
		val second =  async(Dispatchers.IO) {
			delay(10)
			firstSlowAktivitetsService.upsert(
				aktivitetskort(),
				headers(deltakelseId2),
				deltakelseId2,
			)
			excutionOrder.add(deltakelseId2.value)
		}
		listOf(first, second).awaitAll()
		excutionOrder shouldBe listOf(deltakelseId2.value, deltakelseId.value)
	}

})
