package no.nav.helse.spiskammerset.spiskammerset

import no.nav.helse.spiskammerset.spiskammerset.api.Forsikringsgrunnlag
import no.nav.helse.spiskammerset.spiskammerset.db.ForsikringDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class ForsikringDaoTest {

    @Test
    fun `lagrer inntekt`() = databaseTest {
        val forsikringDao = ForsikringDao(it)
        val behandlingId = UUID.randomUUID()
        val forsikring = Forsikringsgrunnlag(UUID.randomUUID(), behandlingId, 80, true)

        forsikringDao.lagre(forsikring)

        val lagretForsikring = forsikringDao.hent(behandlingId)

        assertEquals(forsikring, lagretForsikring)
    }
}
