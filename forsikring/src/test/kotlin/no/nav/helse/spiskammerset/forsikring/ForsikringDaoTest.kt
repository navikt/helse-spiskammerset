package no.nav.helse.spiskammerset.forsikring

import com.github.navikt.tbd_libs.sql_dsl.connection
import no.nav.helse.spiskammerset.forsikring.Forsikring.ArbeidssituasjonForsikringstype.KollektivJordbruksforsikring
import no.nav.helse.spiskammerset.forsikring.Forsikring.ArbeidssituasjonForsikringstype.SelvstendigForsikring
import no.nav.helse.spiskammerset.oppbevaringsboks.Hyllenummer
import no.nav.helse.spiskammerset.oppbevaringsboks.Versjon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

internal class ForsikringDaoTest {

    @Test
    fun `lagrer og henter én forsikring`() = databaseTest { dataSource ->
        dataSource.connection {
            val hyllenummer = opprettHylle()

            val dao = ForsikringDao(this)
            val forsikring = Forsikring(100, true, 500_000, SelvstendigForsikring, Versjon(1))

            dao.lagre(forsikring, hyllenummer)

            val hentetForsikring = dao.hent(hyllenummer)

            assertEquals(forsikring, hentetForsikring)
        }
    }

    @Test
    fun `oppdaterte forsikringsopplysninger på en hylle`() = databaseTest { dataSource ->
        dataSource.connection {
            val hyllenummer = opprettHylle()

            val dao = ForsikringDao(this)
            val forsikring1 = Forsikring(100, false, 500_000, arbeidssituasjonForsikringstype = SelvstendigForsikring, Versjon(1))
            val forsikring2 = Forsikring(100, true, 500_000, arbeidssituasjonForsikringstype = SelvstendigForsikring, Versjon(1))

            dao.lagre(forsikring1, hyllenummer)
            dao.lagre(forsikring2, hyllenummer)

            val hentetForsikring = dao.hent(hyllenummer)

            assertEquals(forsikring2, hentetForsikring)
        }
    }

    @Test
    fun `Lagrer en kollektiv jordbruker forsikring`() = databaseTest { dataSource ->
        dataSource.connection {
            val hyllenummer = opprettHylle()

            val dao = ForsikringDao(this)
            val forsikring = Forsikring(100, false, 0, KollektivJordbruksforsikring, Versjon(1))

            dao.lagre(forsikring, hyllenummer)

            val hentetForsikring = dao.hent(hyllenummer)

            assertEquals(forsikring, hentetForsikring)
        }
    }

    private fun Connection.opprettHylle(): Hyllenummer {
        val sql = """
            INSERT INTO hylle (vedtaksperiode_id, behandling_id, yrkesaktivitetstype, organisasjonsnummer, fom, tom, behandling_opprettet)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING hyllenummer
        """
        return prepareStatement(sql).use { statement ->
            statement.setObject(1, UUID.randomUUID())
            statement.setObject(2, UUID.randomUUID())
            statement.setString(3, "SELVSTENDIG")
            statement.setString(4, null)
            statement.setDate(5, java.sql.Date.valueOf(LocalDate.of(2024, 1, 1)))
            statement.setDate(6, java.sql.Date.valueOf(LocalDate.of(2024, 12, 31)))
            statement.setTimestamp(7, java.sql.Timestamp.from(Instant.parse("2024-01-01T00:00:00Z")))
            val resultSet = statement.executeQuery()
            resultSet.next()
            Hyllenummer(resultSet.getLong("hyllenummer"))
        }
    }
}
