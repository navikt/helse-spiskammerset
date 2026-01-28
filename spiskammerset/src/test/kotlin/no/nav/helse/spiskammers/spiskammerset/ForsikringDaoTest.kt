package no.nav.helse.spiskammers.spiskammerset

import no.nav.helse.spiskammers.spiskammerset.api.Forsikringsgrunnlag
import no.nav.helse.spiskammers.spiskammerset.db.ForsikringDao
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

    @Test
    fun `henter alle forsikringer for en behandling`() = databaseTest {
        val forsikringDao = ForsikringDao(it)
        val behandlingId = UUID.randomUUID()
        val forsikring1 = Forsikringsgrunnlag(UUID.randomUUID(), behandlingId, 80, true)
        val forsikring2 = Forsikringsgrunnlag(UUID.randomUUID(), behandlingId, 100, false)
        val forsikring3 = Forsikringsgrunnlag(UUID.randomUUID(), UUID.randomUUID(), 50, true)

        forsikringDao.lagre(forsikring1)
        forsikringDao.lagre(forsikring2)
        forsikringDao.lagre(forsikring3)

        val hentetForsikringer = forsikringDao.hentAlle(behandlingId)

        assertEquals(2, hentetForsikringer.size)
        assertEquals(setOf(forsikring1, forsikring2), hentetForsikringer.toSet())
    }
}
