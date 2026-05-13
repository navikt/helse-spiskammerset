package no.nav.helse.spiskammerset

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotliquery.queryOf
import kotliquery.sessionOf
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
        sessionOf(dataSource).use { session ->
            session.run(queryOf("TRUNCATE TABLE grunnlagsdata, melding").asUpdate)
        }
    }

    @Test
    fun `melding og forsikring-grunnlagsdata lagres ved mottak av komplett løsning`() {
        // Given:
        val meldingId = UUID.randomUUID()

        val inputJson = """
            {
                "@id": "$meldingId",
                "@behov": ["SelvstendigForsikring", "EtHeltAnnetBehov"],
                "@final": true,
                "@event_name": "behov",
                "@opprettet": "2024-06-01T12:00:00",
                "fødselsnummer": "01020312345",
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
        val uuid = validerGyldigLagringsIdOgGiUUID(selvstendingForsikringLagringsId, "forsikring")

        assertEquals(0, (beriketSelvstendigForsikring["kandidater"][0] as ObjectNode).remove("@index")?.asInt())
        assertEquals(1, (beriketSelvstendigForsikring["kandidater"][1] as ObjectNode).remove("@index")?.asInt())

        // Sammenlikn resten av @løsning-JSON'en (utenom feltene vi har fjernet med .remove() over)
        assertJsonEquals(inputJsonNode["@løsning"], beriketMelding["@løsning"])

        val lagretMelding = fetchMeldingData(meldingId)
        assertNotNull(lagretMelding, "Meldingen skal lagres")
        assertJsonEquals(
            expectedJson = inputJson,
            actualJson = lagretMelding,
            bortsettFraProperties = listOf("system_participating_services", "system_read_count")
        )
        assertEquals(1, countTableRows("melding"))

        val lagretGrunnlagsdata = fetchGrunnlagsdata(uuid.toString(), "forsikring")
        assertNotNull(lagretGrunnlagsdata, "Grunnlagsdata skal lagres")
        assertJsonEquals(rapid.inspektør.message(0)["@løsning"]["SelvstendigForsikring"].toPrettyString(), lagretGrunnlagsdata)
        assertEquals(1, countTableRows("grunnlagsdata"))
    }

    private fun validerGyldigLagringsIdOgGiUUID(lagringsId: String, forventetType: String): UUID {
        val splittetUrn = lagringsId.split(':')
        assertEquals(4, splittetUrn.size, "Feil antall deler i @lagringsId: $lagringsId")

        val (urnKonstant, namespace, type, uuid) = splittetUrn
        assertEquals("urn", urnKonstant)
        assertEquals("grunnlagsdata", namespace)
        assertEquals(forventetType, type)
        return assertDoesNotThrow("Siste del ($uuid) av @lagringsId \"$lagringsId\" kunne ikke tolkes som en UUID") {
            UUID.fromString(uuid)
        }
    }

    private fun fetchMeldingData(meldingId: UUID): String? =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "SELECT data FROM melding WHERE id = :id",
                    mapOf("id" to meldingId)
                ).map { row -> row.string(1) }.asSingle
            )
        }

    private fun fetchGrunnlagsdata(id: String, type: String): String? =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "SELECT data FROM grunnlagsdata WHERE id = :id::uuid AND type = :type",
                    mapOf("id" to id, "type" to type)
                ).map { row -> row.string(1) }.asSingle
            )
        }

    private fun countTableRows(table: String): Int =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf("SELECT COUNT(*) FROM $table").map { row -> row.int(1) }.asSingle
            ) ?: 0
        }

    private fun assertJsonEquals(
        expectedJson: String,
        actualJson: String,
        bortsettFraProperties: List<String> = emptyList()
    ) {
        assertJsonEquals(
            expectedJsonNode = objectMapper.readTree(expectedJson),
            actualJsonNode = objectMapper.readTree(actualJson),
            bortsettFraProperties = bortsettFraProperties
        )
    }

    private fun assertJsonEquals(
        expectedJsonNode: JsonNode,
        actualJsonNode: JsonNode,
        bortsettFraProperties: List<String> = emptyList()
    ) {
        val expected = expectedJsonNode.deepSortedObjectNodeCopy().apply { bortsettFraProperties.forEach { remove(it) } }
        val actual = actualJsonNode.deepSortedObjectNodeCopy().apply { bortsettFraProperties.forEach { remove(it) } }
        assertEquals(
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(expected),
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actual),
        )
    }

    private fun JsonNode.sortedDeep(): JsonNode =
        when (this) {
            is ObjectNode ->
                objectMapper.createObjectNode().also { sorted ->
                    properties().asSequence()
                        .sortedBy { (name, _) -> name }
                        .forEach { (name, value) -> sorted.set<JsonNode>(name, value.sortedDeep()) }
                }

            is ArrayNode ->
                objectMapper.createArrayNode().also { sortedArray ->
                    forEach { sortedArray.add(it.sortedDeep()) }
                }

            else -> this.deepCopy()
        }

    private fun JsonNode.deepSortedObjectNodeCopy(): ObjectNode = sortedDeep() as ObjectNode
}
