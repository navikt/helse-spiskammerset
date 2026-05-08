package no.nav.helse.spiskammerset.opprydding_dev

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.util.*
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.postgresql.PostgreSQLContainer

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SlettPersonRiverTest {
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

    private val rapid = TestRapid().also {
        SlettPersonRiver(it, dataSource)
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
    fun `sletter alle meldinger og grunnlagsdata for en person`() {
        // Given:
        val fødselsnummer = "01010112345"
        val annenPersonsFødselsnummer = "99099912345"
        insertMeldingOgGrunnlagsdata(fødselsnummer)
        insertMeldingOgGrunnlagsdata(annenPersonsFødselsnummer)

        // When:
        rapid.sendTestMessage(
            """
            {
                "@event_name": "slett_person",
                "@id": "${UUID.randomUUID()}",
                "fødselsnummer": "$fødselsnummer"
            }
        """
        )

        // Then:
        assertEquals(0L, countMeldinger(fødselsnummer), "Alle meldinger for person skal være slettet")
        assertEquals(0L, countGrunnlagsdata(fødselsnummer), "All grunnlagsdata for person skal være slettet")

        assertEquals(1L, countMeldinger(annenPersonsFødselsnummer), "Data for andre personer skal ikke berøres")
        assertEquals(1L, countGrunnlagsdata(annenPersonsFødselsnummer), "Data for andre personer skal ikke berøres")
    }

    private fun insertMeldingOgGrunnlagsdata(fødselsnummer: String) {
        val meldingId = UUID.randomUUID()
        dataSource.connection.use { conn ->
            conn.prepareStatement("INSERT INTO melding (id, data) VALUES (?, ?::jsonb)").use { stmt ->
                stmt.setObject(1, meldingId)
                stmt.setString(2, """{ "fødselsnummer": "$fødselsnummer" }""")
                stmt.execute()
            }
            conn.prepareStatement("INSERT INTO grunnlagsdata (data, type, melding_ref) VALUES (?::jsonb, ?, ?)").use { stmt ->
                stmt.setString(1, """{ "noe": "data" }""")
                stmt.setString(2, "TestData")
                stmt.setObject(3, meldingId)
                stmt.execute()
            }
        }
    }

    private fun countMeldinger(fødselsnummer: String): Long =
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "SELECT COUNT(*) FROM melding WHERE data->>'fødselsnummer' = ?"
            ).use { stmt ->
                stmt.setString(1, fødselsnummer)
                stmt.executeQuery().apply { next() }.getLong(1)
            }
        }

    private fun countGrunnlagsdata(fødselsnummer: String): Long =
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "SELECT COUNT(*)" +
                    " FROM grunnlagsdata, melding" +
                    " WHERE grunnlagsdata.melding_ref=melding.id" +
                    " AND melding.data->>'fødselsnummer' = ?"
            ).use { stmt ->
                stmt.setString(1, fødselsnummer)
                stmt.executeQuery().apply { next() }.getLong(1)
            }
        }
}
