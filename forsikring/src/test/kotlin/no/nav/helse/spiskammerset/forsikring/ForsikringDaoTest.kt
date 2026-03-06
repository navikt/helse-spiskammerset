package no.nav.helse.spiskammerset.forsikring

import com.github.navikt.tbd_libs.sql_dsl.connection
import no.nav.helse.spiskammerset.oppbevaringsboks.Versjon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ForsikringDaoTest {

    @Test
    fun `lagrer og henter én forsikring`() = databaseTest { dataSource ->
        dataSource.connection {
            val dao = ForsikringDao(this)
            val forsikring = Forsikring("HundreProsentFraDagEn", 500_000, 1.januar, null, Versjon(1))

            val lagretId = dao.lagre(forsikring)
            val hentetForsikring = dao.hent(lagretId)

            assertEquals(forsikring, hentetForsikring)
        }
    }
}
