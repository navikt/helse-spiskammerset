package no.nav.helse.spiskammerset.forsikring

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.sql_dsl.connection
import no.nav.helse.spiskammerset.oppbevaringsboks.Innhold
import no.nav.helse.spiskammerset.oppbevaringsboks.Versjon
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.skyscreamer.jsonassert.JSONAssert
import java.time.LocalDate
import java.util.*

internal class ForsikringsboksTest {

    @Test
    fun `får ingenting når det ikke finnes noe med innsendt id`() = databaseTest { dataSource ->
        dataSource.connection {
            val hentetInnhold = Forsikringsboks.taUt(UUID.randomUUID(), this)

            assertNull(hentetInnhold)
        }
    }

    @Test
    fun `lagrer og henter ut forsikring`() = databaseTest { dataSource ->
        dataSource.connection {
            val id = Forsikringsboks.puttI(innJson("HundreProsentFraDagEn", 500_000), this)
            val hentetInnhold = Forsikringsboks.taUt(id, this)

            val forventetInnhold = utInnhold(100, 1, 500_000)
            assertLikJsonInnhold(forventetInnhold, hentetInnhold)
        }
    }

    private fun innJson(
        forsikringstype: String,
        premiegrunnlag: Int,
        startdato: LocalDate = 1.januar,
        sluttdato: LocalDate? = null
    ): ObjectNode {
        @Language("JSON")
        val json = """
        {
          "forsikringstype": "$forsikringstype",
          "premiegrunnlag": $premiegrunnlag,
          "startdato": "$startdato",
          "sluttdato": ${ if(sluttdato != null) "$sluttdato" else null }
        }
        """
        return jacksonObjectMapper().readTree(json) as ObjectNode
    }

    private fun utInnhold(dekningsgrad: Int, dag1Eller17: Int, premiegrunnlag: Int) =
        Innhold(
            versjon = Versjon(1),
            innhold = mapOf(
                "dekningsgrad" to dekningsgrad,
                "dag1Eller17" to dag1Eller17,
                "premiegrunnlag" to premiegrunnlag
            )
        )

    private fun assertLikJsonInnhold(expected: Innhold, actual: Innhold?) {
        JSONAssert.assertEquals(expected.tilJson(), actual?.tilJson(), true)
    }
}
