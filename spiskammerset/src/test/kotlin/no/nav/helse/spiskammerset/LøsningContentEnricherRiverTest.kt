package no.nav.helse.spiskammerset

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.testcontainers.postgresql.PostgreSQLContainer

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LøsningContentEnricherRiverTest {
    private val postgres = PostgreSQLContainer("postgres:18").also { it.start() }
    private val dataSource: HikariDataSource =
        HikariDataSource(HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
            maximumPoolSize = 2
        }).also {
            Flyway.configure()
                .dataSource(it)
                .locations("classpath:db/migration")
                .load()
                .migrate()
        }

    private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    private val rapid = TestRapid().also {
        LøsningContentEnricherRiver(it, dataSource)
    }

    @AfterAll
    fun tearDown() {
        dataSource.close()
        postgres.stop()
    }

    @BeforeEach
    fun reset() {
        rapid.reset()
        dataSource.connection.use { conn ->
            conn.prepareStatement("TRUNCATE TABLE grunnlagsdata, melding").execute()
        }
    }

    @Test
    fun `melding og forsikring-grunnlagsdata lagres ved mottak av komplett løsning`() {
        // Given:
        val meldingId = UUID.randomUUID()
        val inputJson = """
            {
                "@event_name": "behov",
                "@behov": ["SelvstendigForsikring", "EtHeltAnnetBehov"],
                "@id": "$meldingId",
                "@opprettet": "2024-06-01T12:00:00",
                "fødselsnummer": "01020312345",
                "@final": true,
                "@lagreLøsninger": true,
                "SelvstendigForsikring": {
                    "skjæringstidspunkt": "2024-05-01"
                },
                "EtHeltAnnetBehov": {
                    "egneGreier": "her"
                },
                "@løsning": {
                    "SelvstendigForsikring": {
                        "kandidater": [
                            {
                                "forsikringstype": "ÅttiProsentFraDagEn",
                                "premiegrunnlag": 12345,
                                "startdato": "2024-01-01",
                                "sluttdato": "2024-12-31"
                            },
                            {
                                "forsikringstype": "HundreProsentFraDagSytten",
                                "premiegrunnlag": 12345,
                                "startdato": "2024-01-01",
                                "sluttdato": "2024-12-31"
                            }
                        ]
                    },
                    "EtHeltAnnetBehov": {
                        "nothingToSee": "here"
                    }
                }
            }
        """.trimIndent()

        // When:
        rapid.sendTestMessage(inputJson)

        // Then:
        assertEquals(1, rapid.inspektør.size)
        val beriketMelding = rapid.inspektør.message(0)

        val inputJsonNode = objectMapper.readTree(inputJson)
        val inputJsonNodeMedLagretTrue = inputJsonNode.deepCopy<ObjectNode>().apply { put("@lagret", true) }
        // Sjekk hele JSON'en bortsett fra R&R-feltene og @løsning
        assertJsonEquals(
            expectedJsonNode = inputJsonNodeMedLagretTrue,
            actualJsonNode = beriketMelding,
            bortsettFraProperties = listOf(
                "@id",
                "@opprettet",
                "system_read_count",
                "system_participating_services",
                "@forårsaket_av",
                "@løsning"
            )
        )

        val beriketSelvstendigForsikring = beriketMelding["@løsning"]["SelvstendigForsikring"] as ObjectNode

        // Kontroller @lagringsId på forsikring
        val selvstendingForsikringLagringsId = beriketSelvstendigForsikring.remove("@lagringsId")?.asText()
        assertNotNull(selvstendingForsikringLagringsId, "Manglet @lagringsId for SelvstendigForsikring")
        erGyldigLagringsId(selvstendingForsikringLagringsId, "forsikring")

        assertEquals(0, (beriketSelvstendigForsikring["kandidater"][0] as ObjectNode).remove("@index")?.asInt())
        assertEquals(1, (beriketSelvstendigForsikring["kandidater"][1] as ObjectNode).remove("@index")?.asInt())

        // Sammenlikn resten av @løsning-JSON'en (utenom feltene vi har fjernet med .remove() over)
        assertJsonEquals(inputJsonNode["@løsning"], beriketMelding["@løsning"])



        /*
        val lagretMelding = fetchMeldingData(meldingId)
        assertNotNull(lagretMelding, "Meldingen skal lagres")
        JSONAssert.assertEquals(inputJson, lagretMelding, JSONCompareMode.STRICT)

        val lagretGrunnlagsdata = objectMapper.readTree(fetchGrunnlagsdata(meldingId, type = "forsikring"))
        assertNotNull(lagretGrunnlagsdata, "Grunnlagsdata av type forsikring skal lagres")
        val lagringsId = lagretGrunnlagsdata["@lagringsId"].asText()
        "urn:grunnlagsdata:forsikring:<uuid goes here>"
        assertEquals("urn:grunnlagsdata:forsikring:")
        JSONAssert.assertEquals(
            """
                [
                    {
                        "forsikringstype": "ÅttiProsentFraDagEn",
                        "premiegrunnlag": 12345,
                        "startdato": "2024-01-01",
                        "sluttdato": "2024-12-31",
                        "@index": 0
                    },
                    {
                        "forsikringstype": "HundreProsentFraDagSytten",
                        "premiegrunnlag": 12345,
                        "startdato": "2024-01-01",
                        "sluttdato": "2024-12-31",
                        "@index": 1
                    }
                ]
                """.trimIndent(),
            lagretGrunnlagsdata["kandidater"].toString(),
            JSONCompareMode.STRICT
        )

         */
    }

    private fun erGyldigLagringsId(lagringsId: String, forventetType: String) {
        val splittetUrn = lagringsId.split(':')
        assertEquals(4, splittetUrn.size, "Feil antall deler i @lagringsId: $lagringsId")

        val (urnKonstant, namespace, type, uuid) = splittetUrn
        assertEquals("urn", urnKonstant)
        assertEquals("grunnlagsdata", namespace)
        assertEquals(forventetType, type)
        assertDoesNotThrow("Siste del ($uuid) av @lagringsId \"$lagringsId\" kunne ikke tolkes som en UUID") {
            UUID.fromString(uuid)
        }
    }

    private fun fetchMeldingData(meldingId: UUID): String? =
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "SELECT data FROM melding WHERE id = ?"
            ).use { stmt ->
                stmt.setObject(1, meldingId)
                val rs = stmt.executeQuery()
                if (rs.next()) rs.getString(1) else null
            }
        }

    private fun fetchGrunnlagsdata(meldingId: UUID, type: String): String? =
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "SELECT data FROM grunnlagsdata WHERE melding_ref = ? AND type = ?"
            ).use { stmt ->
                stmt.setObject(1, meldingId)
                stmt.setString(2, type)
                val rs = stmt.executeQuery()
                if (rs.next()) rs.getString(1) else null
            }
        }

    private fun assertJsonEquals(
        expectedJsonNode: JsonNode,
        actualJsonNode: JsonNode,
        bortsettFraProperties: List<String> = emptyList()
    ) {
        val expectedAsObjectNode =
            expectedJsonNode.deepCopy<ObjectNode>().apply {
                bortsettFraProperties.forEach { remove(it) }
            }
        val actualAsObjectNode =
            actualJsonNode.deepCopy<ObjectNode>().apply {
                bortsettFraProperties.forEach { remove(it) }
            }
        assertEquals(
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(expectedAsObjectNode),
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actualAsObjectNode),
        )
    }
}
