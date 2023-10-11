package no.nav.arena_tiltak_aktivitet_acl.processors

import ArenaOrdsProxyClient
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import no.nav.arena_tiltak_aktivitet_acl.clients.amt_enhetsregister.EnhetsregisterClient
import no.nav.arena_tiltak_aktivitet_acl.clients.amt_enhetsregister.Virksomhet
import no.nav.arena_tiltak_aktivitet_acl.database.DatabaseTestUtils
import no.nav.arena_tiltak_aktivitet_acl.database.SingletonPostgresContainer
import no.nav.arena_tiltak_aktivitet_acl.domain.db.IngestStatus
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Operation
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Tiltak
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.ArenaGjennomforingDto
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.arena.tiltak.ArenaGjennomforingKafkaMessage
import no.nav.arena_tiltak_aktivitet_acl.exceptions.IgnoredException
import no.nav.arena_tiltak_aktivitet_acl.exceptions.ValidationException
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.GjennomforingRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaIdTilAktivitetskortIdRepository
import no.nav.arena_tiltak_aktivitet_acl.services.KafkaProducerService
import no.nav.arena_tiltak_aktivitet_acl.services.TiltakService
import no.nav.arena_tiltak_aktivitet_acl.utils.ArenaTableName
import no.nav.arena_tiltak_aktivitet_acl.utils.ObjectMapper
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArenaGjennomforingProcessorTest {
	private lateinit var jdbcTemplate: NamedParameterJdbcTemplate
	private lateinit var repository: ArenaDataRepository
	private lateinit var arenaIdTilAktivitetskortIdRepository: ArenaIdTilAktivitetskortIdRepository
	private lateinit var tiltakService: TiltakService
	private lateinit var ordsClient: ArenaOrdsProxyClient
	private lateinit var kafkaProducerService: KafkaProducerService
	private lateinit var gjennomforingProcessor: GjennomforingProcessor
	private lateinit var enhetsregisterClient: EnhetsregisterClient

	private var mapper = ObjectMapper.get()

	val dataSource = SingletonPostgresContainer.getDataSource()
	var tiltakKode = "INDOPPFAG"
	var ARBGIV_ID_ARRANGOR = 661733L

	@BeforeAll
	fun beforeAll() {
		jdbcTemplate = NamedParameterJdbcTemplate(dataSource)
		repository = ArenaDataRepository(jdbcTemplate)
		arenaIdTilAktivitetskortIdRepository = ArenaIdTilAktivitetskortIdRepository(jdbcTemplate)
		tiltakService = mock(TiltakService::class.java)
		ordsClient = mock(ArenaOrdsProxyClient::class.java)
		kafkaProducerService = mock(KafkaProducerService::class.java)
		enhetsregisterClient = mock(EnhetsregisterClient::class.java)

		gjennomforingProcessor = GjennomforingProcessor(
			repository,
			GjennomforingRepository(jdbcTemplate),
			ordsClient,
			enhetsregisterClient
		)

		`when`(this.tiltakService.getByKode(tiltakKode)).thenReturn(Tiltak(UUID.randomUUID(), kode=tiltakKode, navn="Oppfølging", administrasjonskode = Tiltak.Administrasjonskode.IND))
		`when`(ordsClient.hentVirksomhetsnummer(ARBGIV_ID_ARRANGOR)).thenReturn("123")
		`when`(enhetsregisterClient.hentVirksomhet("123")).thenReturn(Virksomhet(navn="navn", organisasjonsnummer = "123", overordnetEnhetOrganisasjonsnummer = null,overordnetEnhetNavn = null))
	}

	@BeforeEach
	fun beforeEach() {
		DatabaseTestUtils.cleanDatabase(dataSource)
	}

	@Test
	fun `handleEntry() - Gyldig gjennomforing - inserter i korrekte tabeller`() {
		val opPos = "1"

		val arenaGjennomforingDto = mapper.readValue(arenaGjennomforingJson, ArenaGjennomforingDto::class.java)

		val kafkaMessage = createArenaGjennomforingKafkaMessage(
			operationPosition = opPos,
			arenaGjennomforingDto = arenaGjennomforingDto,
		)

		gjennomforingProcessor.handleArenaMessage(kafkaMessage)

		repository.get(kafkaMessage.arenaTableName, Operation.CREATED, opPos).ingestStatus shouldBe IngestStatus.HANDLED

	}

	@Test
	fun `handleEntry() - ugyldig gjennomforing - skal kaste ValidationException`() {
		val opPos = "2"

		val arenaGjennomforingDto = mapper.readValue(arenaGjennomforingUgyldigJson, ArenaGjennomforingDto::class.java)

		val kafkaMessage = createArenaGjennomforingKafkaMessage(
			operationPosition = opPos,
			arenaGjennomforingDto = arenaGjennomforingDto,
		)

		shouldThrowExactly<ValidationException> {
			gjennomforingProcessor.handleArenaMessage(kafkaMessage)
		}
	}

	@Test
	fun `handleEntry() - operation type delete - skal ignoreres`() {
		val opPos = "11223344"

		val arenaGjennomforingDto = mapper.readValue(arenaGjennomforingJson, ArenaGjennomforingDto::class.java)

		val kafkaMessage = createArenaGjennomforingKafkaMessage(
			operationPosition = opPos,
			operationType = Operation.DELETED,
			arenaGjennomforingDto = arenaGjennomforingDto,
		)

		shouldThrowExactly<IgnoredException > {
			gjennomforingProcessor.handleArenaMessage(kafkaMessage)
		}
	}

	private fun createArenaGjennomforingKafkaMessage(
		operationPosition: String = "1",
		operationType: Operation = Operation.CREATED,
		operationTimestamp: LocalDateTime = LocalDateTime.now(),
		arenaGjennomforingDto: ArenaGjennomforingDto,
	): ArenaGjennomforingKafkaMessage {
		return ArenaGjennomforingKafkaMessage(
			arenaTableName = ArenaTableName.GJENNOMFORING,
			operationType = operationType,
			operationTimestamp = operationTimestamp,
			operationPosition =  operationPosition,
			before = if (listOf(Operation.MODIFIED, Operation.DELETED).contains(operationType)) arenaGjennomforingDto else null,
			after =  if (listOf(Operation.CREATED, Operation.MODIFIED).contains(operationType)) arenaGjennomforingDto else null,
		)
	}

	private val arenaGjennomforingUgyldigJson = """
		{
		  "TILTAKGJENNOMFORING_ID": 830743204,
		  "SAK_ID": 13467550,
		  "TILTAKSKODE": "$tiltakKode",
		  "ANTALL_DELTAKERE": 70,
		  "ANTALL_VARIGHET": null,
		  "DATO_FRA": "2021-12-01 00:00:00",
		  "DATO_TIL": "2023-12-31 00:00:00",
		  "FAGPLANKODE": null,
		  "MAALEENHET_VARIGHET": null,
		  "TEKST_FAGBESKRIVELSE": "asdfds",
		  "TEKST_KURSSTED": null,
		  "TEKST_MAALGRUPPE": "sdfsdf",
		  "STATUS_TREVERDIKODE_INNSOKNING": "J",
		  "REG_DATO": "2021-12-01 10:54:36",
		  "REG_USER": "BRUKER123",
		  "MOD_DATO": "2021-12-01 10:57:19",
		  "MOD_USER": "BRUKER123",
		  "LOKALTNAVN": "testtiltak for komet - oppfølgingstiltak",
		  "TILTAKSTATUSKODE": null,
		  "PROSENT_DELTID": 100,
		  "KOMMENTAR": null,
		  "ARBGIV_ID_ARRANGOR": null,
		  "PROFILELEMENT_ID_GEOGRAFI": null,
		  "KLOKKETID_FREMMOTE": null,
		  "DATO_FREMMOTE": null,
		  "BEGRUNNELSE_STATUS": null,
		  "AVTALE_ID": 315487,
		  "AKTIVITET_ID": 133910244,
		  "DATO_INNSOKNINGSTART": null,
		  "GML_FRA_DATO": null,
		  "GML_TIL_DATO": null,
		  "AETAT_FREMMOTEREG": "0111",
		  "AETAT_KONTERINGSSTED": "0111",
		  "OPPLAERINGNIVAAKODE": null,
		  "TILTAKGJENNOMFORING_ID_REL": null,
		  "VURDERING_GJENNOMFORING": null,
		  "PROFILELEMENT_ID_OPPL_TILTAK": null,
		  "DATO_OPPFOLGING_OK": null,
		  "PARTISJON": null,
		  "MAALFORM_KRAVBREV": "NO"
		}
	""".trimIndent()

	private val arenaGjennomforingJson = """
		{
		  "TILTAKGJENNOMFORING_ID": 3728063,
		  "SAK_ID": 13467550,
		  "TILTAKSKODE": "$tiltakKode",
		  "ANTALL_DELTAKERE": 70,
		  "ANTALL_VARIGHET": null,
		  "DATO_FRA": "2021-12-01 00:00:00",
		  "DATO_TIL": "2023-12-31 00:00:00",
		  "FAGPLANKODE": null,
		  "MAALEENHET_VARIGHET": null,
		  "TEKST_FAGBESKRIVELSE": "asdfds",
		  "TEKST_KURSSTED": null,
		  "TEKST_MAALGRUPPE": "sdfsdf",
		  "STATUS_TREVERDIKODE_INNSOKNING": "J",
		  "REG_DATO": "2021-12-01 10:54:36",
		  "REG_USER": "BRUKER123",
		  "MOD_DATO": "2021-12-01 10:57:19",
		  "MOD_USER": "BRUKER123",
		  "LOKALTNAVN": "testtiltak for komet - oppfølgingstiltak",
		  "TILTAKSTATUSKODE": "GJENNOMFOR",
		  "PROSENT_DELTID": 100,
		  "KOMMENTAR": null,
		  "ARBGIV_ID_ARRANGOR": $ARBGIV_ID_ARRANGOR,
		  "PROFILELEMENT_ID_GEOGRAFI": null,
		  "KLOKKETID_FREMMOTE": null,
		  "DATO_FREMMOTE": null,
		  "BEGRUNNELSE_STATUS": null,
		  "AVTALE_ID": 315487,
		  "AKTIVITET_ID": 133910244,
		  "DATO_INNSOKNINGSTART": null,
		  "GML_FRA_DATO": null,
		  "GML_TIL_DATO": null,
		  "AETAT_FREMMOTEREG": "0111",
		  "AETAT_KONTERINGSSTED": "0111",
		  "OPPLAERINGNIVAAKODE": null,
		  "TILTAKGJENNOMFORING_ID_REL": null,
		  "VURDERING_GJENNOMFORING": null,
		  "PROFILELEMENT_ID_OPPL_TILTAK": null,
		  "DATO_OPPFOLGING_OK": null,
		  "PARTISJON": null,
		  "MAALFORM_KRAVBREV": "NO"
		}
	""".trimIndent()


}
