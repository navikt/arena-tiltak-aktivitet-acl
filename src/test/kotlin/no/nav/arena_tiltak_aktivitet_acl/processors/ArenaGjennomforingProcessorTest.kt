package no.nav.arena_tiltak_aktivitet_acl.processors

import ArenaOrdsProxyClient
import io.mockk.mockk
import no.nav.arena_tiltak_aktivitet_acl.database.DatabaseTestUtils
import no.nav.arena_tiltak_aktivitet_acl.database.SingletonPostgresContainer
import no.nav.arena_tiltak_aktivitet_acl.domain.kafka.aktivitet.Tiltak
import no.nav.arena_tiltak_aktivitet_acl.repositories.TranslationRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.ArenaDataRepository
import no.nav.arena_tiltak_aktivitet_acl.repositories.GjennomforingRepository
import no.nav.arena_tiltak_aktivitet_acl.services.KafkaProducerService
import no.nav.arena_tiltak_aktivitet_acl.services.TiltakService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArenaGjennomforingProcessorTest {
	private lateinit var jdbcTemplate: NamedParameterJdbcTemplate
	private lateinit var repository: ArenaDataRepository
	private lateinit var translationRepository: TranslationRepository
	private lateinit var tiltakService: TiltakService
	private lateinit var ordsClient: ArenaOrdsProxyClient
	private lateinit var kafkaProducerService: KafkaProducerService
	private lateinit var gjennomforingProcessor: GjennomforingProcessor

	val dataSource = SingletonPostgresContainer.getDataSource()
	var tiltakKode = "INDOPPFAG"
	var ARBGIV_ID_ARRANGOR = 661733L

	@BeforeAll
	fun beforeAll() {
		jdbcTemplate = NamedParameterJdbcTemplate(dataSource)
		repository = ArenaDataRepository(jdbcTemplate)
		translationRepository = TranslationRepository(jdbcTemplate)
		tiltakService = mock(TiltakService::class.java)
		ordsClient = mock(ArenaOrdsProxyClient::class.java)
		kafkaProducerService = mock(KafkaProducerService::class.java)

		gjennomforingProcessor = GjennomforingProcessor(
			repository,
			GjennomforingRepository(jdbcTemplate),
			ordsClient,
			mockk()
		)

		`when`(this.tiltakService.getByKode(tiltakKode)).thenReturn(Tiltak(UUID.randomUUID(), kode=tiltakKode, navn="Oppfølging", administrasjonskode = Tiltak.Administrasjonskode.IND))
		`when`(ordsClient.hentVirksomhetsnummer(ARBGIV_ID_ARRANGOR)).thenReturn("123")
	}

	@BeforeEach
	fun beforeEach() {
		DatabaseTestUtils.cleanDatabase(dataSource)
	}
/*
	@Test
	fun `handleEntry() - Gyldig gjennomføring - inserter i korrekte tabeller`() {
		val opPos = "1"

		val arenaGjennomforingDto = mapper.readValue(arenaGjennomforingJson, ArenaGjennomforingDto::class.java)

		val kafkaMessage = createArenaGjennomforingKafkaMessage(
			operationPosition = opPos,
			arenaGjennomforingDto = arenaGjennomforingDto,
		)

		gjennomforingProcessor.handleArenaMessage(kafkaMessage)

		val translationData = translationRepository.get(kafkaMessage.arenaTableName, "3728063")
		translationData!!.arenaId shouldBe "3728063"
		translationData.ignored shouldBe false

		repository.get(kafkaMessage.arenaTableName, Operation.CREATED, opPos).ingestStatus shouldBe IngestStatus.HANDLED

	}

	@Test
	fun `handleEntry() - ugyldig gjennomføring - skal kaste ValidationException`() {
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
	fun `handleEntry() - tiltaktype er ikke oppfølging - skal kaste IgnoredException`() {
		val opPos = "2"

		val arenaGjennomforingDto = mapper.readValue(arenaGjennomforingUkjentTypeJson, ArenaGjennomforingDto::class.java)

		val kafkaMessage = createArenaGjennomforingKafkaMessage(
			operationPosition = opPos,
			arenaGjennomforingDto = arenaGjennomforingDto,
		)

		`when`(tiltakService.getByKode(ukjentTiltakType)).thenReturn(Tiltak(UUID.randomUUID(), kode=tiltakKode, navn="Oppfølging"))

		shouldThrowExactly<IgnoredException> {
			gjennomforingProcessor.handleArenaMessage(kafkaMessage)
		}
	}

	@Test
	fun `handleEntry() - operation type delete - skal sendes videre`() {
		val opPos = "11223344"

		val arenaGjennomforingDto = mapper.readValue(arenaGjennomforingJson, ArenaGjennomforingDto::class.java)

		val kafkaMessage = createArenaGjennomforingKafkaMessage(
			operationPosition = opPos,
			operationType = Operation.DELETED,
			arenaGjennomforingDto = arenaGjennomforingDto,
		)

		gjennomforingProcessor.handleArenaMessage(kafkaMessage)

		val translationData = translationRepository.get(ARENA_GJENNOMFORING_TABLE_NAME, "3728063")
		translationData shouldNotBe null

		repository.get(ARENA_GJENNOMFORING_TABLE_NAME, Operation.DELETED, opPos).ingestStatus shouldBe IngestStatus.HANDLED
	}

	private fun createArenaGjennomforingKafkaMessage(
		operationPosition: String = "1",
		operationType: Operation = Operation.CREATED,
		operationTimestamp: LocalDateTime = LocalDateTime.now(),
		arenaGjennomforingDto: ArenaGjennomforingDto,
	): ArenaGjennomforingKafkaMessage {
		return ArenaGjennomforingKafkaMessage(
			arenaTableName =  ARENA_GJENNOMFORING_TABLE_NAME,
			operationType = operationType,
			operationTimestamp = operationTimestamp,
			operationPosition =  operationPosition,
			before = if (listOf(Operation.MODIFIED, Operation.DELETED).contains(operationType)) arenaGjennomforingDto else null,
			after =  if (listOf(Operation.CREATED, Operation.MODIFIED).contains(operationType)) arenaGjennomforingDto else null,
		)
	}*/

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
		  "TILTAKSTATUSKODE": "GJENNOMFOR",
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

	private val arenaGjennomforingUkjentTypeJson = """
		{
		  "TILTAKGJENNOMFORING_ID": 7843295,
		  "SAK_ID": 13467550,
		  "TILTAKSKODE": "ENNYTYPETILTAK",
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
