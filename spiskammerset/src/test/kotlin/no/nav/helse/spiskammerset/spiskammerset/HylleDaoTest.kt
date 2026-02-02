package no.nav.helse.spiskammerset.spiskammerset

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.spiskammerset.spiskammerset.reisverk.Behandling
import no.nav.helse.spiskammerset.spiskammerset.reisverk.BehandlingId
import no.nav.helse.spiskammerset.spiskammerset.reisverk.FunnetHylle
import no.nav.helse.spiskammerset.spiskammerset.reisverk.Organisasjonsnummer
import no.nav.helse.spiskammerset.spiskammerset.reisverk.Periode
import no.nav.helse.spiskammerset.spiskammerset.reisverk.Personidentifikator
import no.nav.helse.spiskammerset.spiskammerset.reisverk.VedtaksperiodeId
import no.nav.helse.spiskammerset.spiskammerset.reisverk.Yrkesaktivitetstype
import no.nav.helse.spiskammerset.spiskammerset.reisverk.finnHyller
import no.nav.helse.spiskammerset.spiskammerset.reisverk.finnRettHylle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HylleDaoTest {

    @Test
    fun `To hendelser for samme behandling gir samme hyllenummer`() = databaseTest { dataSource ->
        val behandlingFraHendelse1 = lagKomplettBehandling()
        val behandlingFraHendelse2 = behandlingFraHendelse1.copy(
            periode = Periode(
                fom = LocalDate.parse("2018-01-02"),
                tom = LocalDate.parse("2018-01-30")
            )
        )
        val personidentifikator = Personidentifikator("11111111111")

        dataSource.connection.use { connection ->
            val hyllenummer1 = connection.finnRettHylle(
                personidentifikator = personidentifikator,
                behandling = behandlingFraHendelse1
            ).hyllenummer

            val hyllenummer2 = connection.finnRettHylle(
                personidentifikator = personidentifikator,
                behandling = behandlingFraHendelse2
            ).hyllenummer

            assertEquals(hyllenummer1, hyllenummer2)
        }
    }

    @Test
    fun `Finne hyller på person + periode`() = databaseTest { dataSource ->
        val personidentifikator = Personidentifikator("12345678910")
        val opprettetBehandling = lagKomplettBehandling(fom = 2.januar, tom = 30.januar)

        dataSource.connection.use { connection ->
            connection.finnRettHylle(
                personidentifikator = personidentifikator,
                behandling = opprettetBehandling
            )

            assertEquals(emptyList<FunnetHylle>(), connection.finnHyller(Periode(1.januar, 1.januar), personidentifikator)) // før
            assertEquals(opprettetBehandling, connection.finnHyller(Periode(2.januar, 2.januar), personidentifikator).single().behandling) // snute
            assertEquals(opprettetBehandling, connection.finnHyller(Periode(15.januar, 15.januar), personidentifikator).single().behandling) // mage
            assertEquals(opprettetBehandling, connection.finnHyller(Periode(30.januar, 30.januar), personidentifikator).single().behandling) // hale
            assertEquals(emptyList<FunnetHylle>(), connection.finnHyller(Periode(31.januar, 31.januar), personidentifikator)) // etter
        }
    }

    private fun lagKomplettBehandling(fom: LocalDate = 1.januar, tom: LocalDate = 31.januar) = Behandling.KomplettBehandling(
        vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID()),
        behandlingId = BehandlingId(UUID.randomUUID()),
        periode = Periode(fom, tom),
        yrkesaktivitetstype = Yrkesaktivitetstype("ARBEIDSTAKER"),
        organisasjonsnummer = Organisasjonsnummer("999999999")
    )
}
