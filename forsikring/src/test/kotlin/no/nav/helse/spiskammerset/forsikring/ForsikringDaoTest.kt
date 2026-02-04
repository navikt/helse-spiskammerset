package no.nav.helse.spiskammerset.forsikring

import com.github.navikt.tbd_libs.sql_dsl.connection
import no.nav.helse.spiskammerset.oppbevaringsboks.Hyllenummer
import no.nav.helse.spiskammerset.oppbevaringsboks.Versjon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ForsikringDaoTest {

    @Test
    fun `lagrer og henter én forsikring`() = databaseTest { dataSource ->
        dataSource.connection {
            val dao = ForsikringDao(this)
            val forsikring = Forsikring(100, true, 500_000, Versjon(1))

            dao.lagre(forsikring, Hyllenummer(1))

            val hentetForsikring = dao.hent(Hyllenummer(1))

            assertEquals(forsikring, hentetForsikring)
        }
    }

    @Test
    fun `oppdaterte forsikringsopplysninger på en hylle`() = databaseTest { dataSource ->
        dataSource.connection {
            val dao = ForsikringDao(this)
            val forsikring1 = Forsikring(100, false, 500_000, Versjon(1))
            val forsikring2 = Forsikring(100, true, 500_000, Versjon(1))

            dao.lagre(forsikring1, Hyllenummer(1))
            dao.lagre(forsikring2, Hyllenummer(1))

            val hentetForsikring = dao.hent(Hyllenummer(1))

            assertEquals(forsikring2, hentetForsikring)
        }
    }
}