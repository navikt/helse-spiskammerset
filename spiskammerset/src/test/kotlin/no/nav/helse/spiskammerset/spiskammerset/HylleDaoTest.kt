package no.nav.helse.spiskammerset.spiskammerset

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.spiskammerset.spiskammerset.reisverk.Behandling
import no.nav.helse.spiskammerset.spiskammerset.reisverk.BehandlingId
import no.nav.helse.spiskammerset.spiskammerset.reisverk.FunnetHylle
import no.nav.helse.spiskammerset.spiskammerset.reisverk.Hyllestatus
import no.nav.helse.spiskammerset.spiskammerset.reisverk.Organisasjonsnummer
import no.nav.helse.spiskammerset.spiskammerset.reisverk.Periode
import no.nav.helse.spiskammerset.spiskammerset.reisverk.Personidentifikator
import no.nav.helse.spiskammerset.spiskammerset.reisverk.VedtaksperiodeId
import no.nav.helse.spiskammerset.spiskammerset.reisverk.Yrkesaktivitetstype
import no.nav.helse.spiskammerset.spiskammerset.reisverk.finnHyller
import no.nav.helse.spiskammerset.spiskammerset.reisverk.finnEllerOpprettHylle
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

        dataSource.connection.use { connection ->
            val hyllenummer1 = connection.finnEllerOpprettHylle(
                behandling = behandlingFraHendelse1
            ).hyllenummer

            val hyllenummer2 = connection.finnEllerOpprettHylle(
                behandling = behandlingFraHendelse2
            ).hyllenummer

            assertEquals(hyllenummer1, hyllenummer2)
        }
    }

    @Test
    fun `Finne hyller på person + periode`() = databaseTest { dataSource ->
        val personidentifikator = Personidentifikator("12345678910")
        val opprettetBehandling = lagKomplettBehandling(fom = 2.januar, tom = 30.januar, personidentifikator = personidentifikator)

        dataSource.connection.use { connection ->
            connection.finnEllerOpprettHylle(
                behandling = opprettetBehandling
            )

            assertEquals(emptyList<FunnetHylle>(), connection.finnHyller(Periode(1.januar, 1.januar), personidentifikator)) // før
            assertEquals(opprettetBehandling, connection.finnHyller(Periode(2.januar, 2.januar), personidentifikator).single().behandling) // snute
            assertEquals(opprettetBehandling, connection.finnHyller(Periode(15.januar, 15.januar), personidentifikator).single().behandling) // mage
            assertEquals(opprettetBehandling, connection.finnHyller(Periode(30.januar, 30.januar), personidentifikator).single().behandling) // hale
            assertEquals(emptyList<FunnetHylle>(), connection.finnHyller(Periode(31.januar, 31.januar), personidentifikator)) // etter
        }
    }

    @Test
    fun `En hyllereise`() = databaseTest { dataSource ->
        val behandlingFraHendelse1 = lagKomplettBehandling()
        val behandlingFraHendelse2 = behandlingFraHendelse1.copy(
            periode = Periode(2.januar, 30.januar)
        )
        val behandlingFrahendelse3 = behandlingFraHendelse2.copy()

        dataSource.connection.use { connection ->
            val hyllestatus1 = connection.finnEllerOpprettHylle(
                behandling = behandlingFraHendelse1
            )
            val hyllenummer = hyllestatus1.hyllenummer
            assertEquals(Hyllestatus.NyHylle::class, hyllestatus1::class)

            assertEquals(Hyllestatus.EndretHylle(hyllenummer), connection.finnEllerOpprettHylle(
                behandling = behandlingFraHendelse2
            ))

            assertEquals(Hyllestatus.UendretHylle(hyllenummer), connection.finnEllerOpprettHylle(
                behandling = behandlingFrahendelse3
            ))

            assertEquals(Hyllestatus.EndretHylle(hyllenummer), connection.finnEllerOpprettHylle(
                behandling = lagMinimalBehandling(
                    behandlingId = behandlingFraHendelse1.behandlingId,
                    periode = Periode(3.januar, 30.januar)
                )
            ))

            assertEquals(Hyllestatus.UendretHylle(hyllenummer), connection.finnEllerOpprettHylle(
                behandling = lagMinimalBehandling(
                    behandlingId = behandlingFraHendelse1.behandlingId
                )
            ))

            assertEquals(Hyllestatus.UendretHylle(hyllenummer), connection.finnEllerOpprettHylle(
                behandling = lagMinimalBehandling(
                    behandlingId = behandlingFraHendelse1.behandlingId,
                    periode = Periode(3.januar, 30.januar)
                )
            ))
        }
    }

    private fun lagKomplettBehandling(fom: LocalDate = 1.januar, tom: LocalDate = 31.januar, personidentifikator: Personidentifikator = Personidentifikator("11111111111")) = Behandling.KomplettBehandling(
        vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID()),
        behandlingId = BehandlingId(UUID.randomUUID()),
        periode = Periode(fom, tom),
        yrkesaktivitetstype = Yrkesaktivitetstype("ARBEIDSTAKER"),
        organisasjonsnummer = Organisasjonsnummer("999999999"),
        personidentifikator = personidentifikator
    )

    private fun lagMinimalBehandling(behandlingId: BehandlingId, periode: Periode? = null) = Behandling.MinimalBehandling(
        behandlingId = behandlingId,
        periode = periode
    )
}
