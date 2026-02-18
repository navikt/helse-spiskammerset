package no.nav.helse.spiskammerset.forsikring

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.sql_dsl.connection
import no.nav.helse.spiskammerset.oppbevaringsboks.Hyllenummer
import no.nav.helse.spiskammerset.oppbevaringsboks.Innhold
import no.nav.helse.spiskammerset.oppbevaringsboks.Innholdsstatus
import no.nav.helse.spiskammerset.oppbevaringsboks.Versjon
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.skyscreamer.jsonassert.JSONAssert
import java.sql.Connection
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

internal class ForsikringsboksTest {

    @Test
    fun `henter ut forsikringsinformasjon fra benyttet_grunnlagsdata_for_beregning`() = databaseTest { dataSource ->
        dataSource.connection {
            val hyllenummer = opprettHylle()
            Forsikringsboks.leggPå(hyllenummer, innJson(100, true, 500_000, "SelvstendigForsikring"), this)

            val hentetInnhold = Forsikringsboks.taNedFra(hyllenummer, this)
            assertLikJsonInnhold(utInnhold(100,1), hentetInnhold)
        }
    }

    @Test
    fun `ignorerer andre events`() = databaseTest { dataSource ->
        dataSource.connection {
            val hyllenummer = opprettHylle()
            val status = Forsikringsboks.leggPå(hyllenummer, innJson(100, true, 500_000, "SelvstendigForsikring", "mjau"), this)

            assertEquals(Innholdsstatus.UendretInnhold, status)
        }
    }

    @Test
    fun `får ingenting når det ikke finnes noe på hylla`() = databaseTest { dataSource ->
        dataSource.connection {
            val hyllenummer = opprettHylle()
            val hentetInnhold = Forsikringsboks.taNedFra(hyllenummer, this)

            assertNull(hentetInnhold)
        }
    }

    @Test
    fun `hva skjer når samme data forsøkes lagres to ganger`() = databaseTest { dataSource ->
        dataSource.connection {
            val hyllenummer = opprettHylle()
            Forsikringsboks.leggPå(hyllenummer, innJson(100, true, 500_000, "SelvstendigForsikring"), this)
            val status = Forsikringsboks.leggPå(hyllenummer, innJson(100, true, 500_000, "SelvstendigForsikring"), this)

            assertEquals(Innholdsstatus.EndretInnhold, status) // Innholdet er vel egentlig ikke endret? er det litt rart eller?
        }
    }

    private fun innJson(
        dekningsgrad: Int,
        navOvertarAnsvarForVentetid: Boolean,
        premiegrunnlag: Int,
        arbeidssituasjonForsikringstype: String,
        eventName: String = "benyttet_grunnlagsdata_for_beregning"
    ): ObjectNode {
        @Language("JSON")
        val json = """
        {
            "@event_name": "$eventName",
            "forsikring": {
                "dekningsgrad": $dekningsgrad,
                "navOvertarAnsvarForVentetid": $navOvertarAnsvarForVentetid,
                "premiegrunnlag": $premiegrunnlag,
                "arbeidssituasjonForsikringstype": "$arbeidssituasjonForsikringstype"
            }
        }
        """
        return jacksonObjectMapper().readTree(json) as ObjectNode
    }

    private fun utInnhold(dekningsgrad: Int, dag1Eller17: Int) = Innhold(versjon = Versjon(1), innhold = mapOf(
        "dekningsgrad" to dekningsgrad,
        "dag1Eller17" to dag1Eller17
    ))

    private fun assertLikJsonInnhold(expected: Innhold, actual: Innhold?) {
        JSONAssert.assertEquals(expected.tilJson(), actual?.tilJson(), true)
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
