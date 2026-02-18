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

            val insertForsikringSql = """INSERT INTO forsikring (dekningsgrad, nav_overtar_ansvar_for_ventetid, premiegrunnlag, arbeidssituasjonForsikringstype, hyllenummer, versjon)
                VALUES (:dekningsgrad, :nav_overtar_ansvar_for_ventetid, :premiegrunnlag, :arbeidssituasjonForsikringstype, :hyllenummer, :versjon)"""
            prepareStatementWithNamedParameters(insertForsikringSql) {
                withParameter("dekningsgrad", 100)
                withParameter("nav_overtar_ansvar_for_ventetid", true)
                withParameter("premiegrunnlag", 500_000)
                withParameter("arbeidssituasjonForsikringstype", "SelvstendigForsikring")
                withParameter("hyllenummer", 1)
                withParameter("versjon", 1)
            }.use(PreparedStatement::executeUpdate)

            val insertHendelseSql = """INSERT INTO hendelse (hendelse_id, hendelsetype, hendelse)
                VALUES (:hendelseId, :hendelsetype, to_jsonb(:hendelse))"""
            prepareStatementWithNamedParameters(insertHendelseSql) {
                withParameter("hendelseId", UUID.randomUUID())
                withParameter("hendelsetype", "behandling_opprettet")
                withParameter("hendelse", """{"@event_name": "behandling_opprettet", "fødselsnummer": "11111111111"}""")
            }.use(PreparedStatement::executeUpdate)

            val insertHendelserPaaHyllaSql = "INSERT INTO hendelser_paa_hylla (intern_hendelse_id, hyllenummer) VALUES (1, 1)"
            prepareStatement(insertHendelserPaaHyllaSql).use { statement ->
                statement.executeUpdate()
            }

            val insertHylleeierSql = "INSERT INTO hylleeier (personidentifikator, hyllenummer) VALUES ('11111111111', 1)"
            prepareStatement(insertHylleeierSql).use { statement ->
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

            val countForsikring = prepareStatement("SELECT COUNT(1) FROM forsikring").use { statement ->
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        resultSet.getInt(1)
                    } else {
                        0
                    }
                }
            }

            val countHendelse = prepareStatement("SELECT COUNT(1) FROM hendelse").use { statement ->
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        resultSet.getInt(1)
                    } else {
                        0
                    }
                }
            }

            val countHendelserPaaHylla = prepareStatement("SELECT COUNT(1) FROM hendelser_paa_hylla").use { statement ->
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        resultSet.getInt(1)
                    } else {
                        0
                    }
                }
            }

            assertEquals(1, countForsikring)
            assertEquals(1, countHylle)
            assertEquals(1, countHendelse)
            assertEquals(1, countHendelserPaaHylla)
        }

        testRapid.sendTestMessage("""{"@event_name": "slett_person", "fødselsnummer": "11111111111"}""")

        dataSource.connection {
            val countHylleeier = prepareStatement("SELECT COUNT(1) FROM hylleeier WHERE personidentifikator = '11111111111'").use { statement ->
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        resultSet.getInt(1)
                    } else {
                        0
                    }
                }
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

            val countForsikring = prepareStatement("SELECT COUNT(1) FROM forsikring").use { statement ->
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        resultSet.getInt(1)
                    } else {
                        0
                    }
                }
            }

            val countHendelserPaaHylla = prepareStatement("SELECT COUNT(1) FROM hendelser_paa_hylla").use { statement ->
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        resultSet.getInt(1)
                    } else {
                        0
                    }
                }
            }

            val countHendelse = prepareStatement("SELECT COUNT(1) FROM hendelse").use { statement ->
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        resultSet.getInt(1)
                    } else {
                        0
                    }
                }
            }
            // Hylleeier skal være slettet
            assertEquals(0, countHylleeier)
            // Hylle skal være slettet
            assertEquals(0, countHylle)
            // Forsikring skal være slettet (eksplisitt slettet)
            assertEquals(0, countForsikring)
            // Hendelser_paa_hylla skal være slettet (CASCADE DELETE fra hylle)
            assertEquals(0, countHendelserPaaHylla)
            // Hendelse skal fortsatt eksistere (vi sletter bare koblingen, ikke selve hendelsen)
            assertEquals(1, countHendelse)
        }
    }
}
