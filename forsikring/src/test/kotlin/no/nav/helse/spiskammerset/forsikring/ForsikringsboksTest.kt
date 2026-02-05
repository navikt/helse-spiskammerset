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

internal class ForsikringsboksTest {

    @Test
    fun `henter ut forsikringsinformasjon fra benyttet_grunnlagsdata_for_beregning`() = databaseTest { dataSource ->
        dataSource.connection {
            Forsikringsboks.leggPå(Hyllenummer(1), innJson(100, true, 500_000), this)

            val hentetInnhold = Forsikringsboks.taNedFra(Hyllenummer(1), this)
            val forventetInnhold = Innhold(Versjon(1), utJson(100, 1))
            assertEquals(forventetInnhold, hentetInnhold)
        }
    }

    @Test
    fun `ignorerer andre events`() = databaseTest { dataSource ->
        dataSource.connection {
            val status = Forsikringsboks.leggPå(Hyllenummer(1), innJson(100, true, 500_000, "mjau"), this)

            assertEquals(Innholdsstatus.UendretInnhold, status)
        }
    }

    @Test
    fun `får ingenting når det ikke finnes noe på hylla`() = databaseTest { dataSource ->
        dataSource.connection {
            val hentetInnhold = Forsikringsboks.taNedFra(Hyllenummer(1), this)

            assertNull(hentetInnhold)
        }
    }

    @Test
    fun `hva skjer når samme data forsøkes lagres to ganger`() = databaseTest { dataSource ->
        dataSource.connection {
            Forsikringsboks.leggPå(Hyllenummer(1), innJson(100, true, 500_000), this)
            val status = Forsikringsboks.leggPå(Hyllenummer(1), innJson(100, true, 500_000), this)

            assertEquals(Innholdsstatus.EndretInnhold, status) // Innholdet er vel egentlig ikke endret? er det litt rart eller?
        }
    }

    private fun innJson(dekningsgrad: Int, navOvertarAnsvarForVentetid: Boolean, premiegrunnlag: Int, eventName: String = "benyttet_grunnlagsdata_for_beregning"): ObjectNode {
        @Language("JSON")
        val json = """
        {
            "@event_name": "$eventName",
            "forsikring": {
                "dekningsgrad": $dekningsgrad,
                "navOvertarAnsvarForVentetid": $navOvertarAnsvarForVentetid,
                "premiegrunnlag": $premiegrunnlag
            }
        }
        """
        return jacksonObjectMapper().readTree(json) as ObjectNode
    }

    private fun utJson(dekningsgrad: Int, dag1Eller17: Int): ObjectNode {
        @Language("JSON")
        val json = """
        {
            "dekningsgrad": $dekningsgrad,
            "dag1Eller17": $dag1Eller17
        }
        """
        return jacksonObjectMapper().readTree(json) as ObjectNode
    }
}
