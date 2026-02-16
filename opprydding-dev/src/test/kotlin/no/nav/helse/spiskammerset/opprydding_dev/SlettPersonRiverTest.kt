package no.nav.helse.spiskammerset.opprydding_dev

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.PreparedStatement
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.use

internal class SlettPersonRiverTest {

    private lateinit var testRapid: TestRapid

    @BeforeEach
    fun beforeEach() {
        testRapid = TestRapid()
    }

    @Test
    fun `sletter hylleeier når meldingen kommer inn`() = databaseTest { dataSource ->
        SlettPersonRiver(testRapid, dataSource)

        dataSource.connection {
            val insertHylleSql = """INSERT INTO hylle (vedtaksperiode_id, behandling_id, yrkesaktivitetstype, organisasjonsnummer, fom, tom, behandling_opprettet) 
                VALUES (:vedtaksperiodeId, :behandlingId, :yrkesaktivitetstype, :organisasjonsnummer, :fom, :tom, :behandlingOpprettet)"""
            prepareStatementWithNamedParameters(insertHylleSql) {
                withParameter("vedtaksperiodeId", UUID.randomUUID())
                withParameter("behandlingId", UUID.randomUUID())
                withParameter("yrkesaktivitetstype", "SELVSTENDIG")
                withNull("organisasjonsnummer")
                withParameter("fom", LocalDate.parse("2024-01-01"))
                withParameter("tom", LocalDate.parse("2024-12-31"))
                withParameter("behandlingOpprettet", Instant.parse("2024-01-01T00:00:00Z"))
            }.use(PreparedStatement::executeUpdate)


            val insertSql = "INSERT INTO hylleeier (personidentifikator, hyllenummer) VALUES ('11111111111', 1)"
            prepareStatement(insertSql).use { statement ->
                statement.executeUpdate()
            }

            val countHylle = prepareStatement("SELECT COUNT(1) FROM hylle").use { statement ->
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        resultSet.getInt(1)
                    } else {
                        0
                    }
                }
            }
            assertEquals(1, countHylle)
        }

        testRapid.sendTestMessage("""{"@event_name": "slett_person", "fødselsnummer": "11111111111"}""")

        dataSource.connection {
            val count = prepareStatement("SELECT COUNT(1) FROM hylleeier WHERE personidentifikator = '11111111111'").use { statement ->
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        resultSet.getInt(1)
                    } else {
                        0
                    }
                }
            }
            assertEquals(0, count)

            val countHylle = prepareStatement("SELECT COUNT(1) FROM hylle").use { statement ->
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        resultSet.getInt(1)
                    } else {
                        0
                    }
                }
            }
            // TODO: Denne burde vært 0, da hylleeieren er slettet og det ikke skal være noen hylle uten eier
            assertEquals(1, countHylle)
        }
    }
}
