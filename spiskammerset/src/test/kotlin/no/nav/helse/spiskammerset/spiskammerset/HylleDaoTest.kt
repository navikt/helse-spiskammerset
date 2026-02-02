package no.nav.helse.spiskammerset.spiskammerset

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.spiskammerset.spiskammerset.reisverk.Behandling
import no.nav.helse.spiskammerset.spiskammerset.reisverk.BehandlingId
import no.nav.helse.spiskammerset.spiskammerset.reisverk.HendelseId
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
import org.junit.jupiter.api.assertThrows

class HylleDaoTest {

    @Test
    fun `To hendelser for samme behandling gir samme hyllenummer`() = databaseTest { dataSource ->
        val hendelseId1 = HendelseId(UUID.randomUUID())
        val hendelseId2 = HendelseId(UUID.randomUUID())

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
                hendelseId = hendelseId1,
                personidentifikator = personidentifikator,
                behandling = behandlingFraHendelse1
            )

            val hyllenummer2 = connection.finnRettHylle(
                hendelseId = hendelseId2,
                personidentifikator = personidentifikator,
                behandling = behandlingFraHendelse2
            )

            assertEquals(hyllenummer1, hyllenummer2)
        }
    }

    @Test
    fun `En behandling som plutselig får melding på en annen vedtaksperiode`() = databaseTest { dataSource ->
        val hendelseId1 = HendelseId(UUID.randomUUID())
        val hendelseId2 = HendelseId(UUID.randomUUID())

        val behandlingFraHendelse1 = lagKomplettBehandling()
        val behandlingFraHendelse2 = behandlingFraHendelse1.copy(
            vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID())
        )
        val personidentifikator = Personidentifikator("11111111111")

        dataSource.connection.use { connection ->
            connection.finnRettHylle(
                hendelseId = hendelseId1,
                personidentifikator = personidentifikator,
                behandling = behandlingFraHendelse1
            )

            assertThrows<NoSuchElementException> { connection.finnRettHylle(
                hendelseId = hendelseId2,
                personidentifikator = personidentifikator,
                behandling = behandlingFraHendelse2
            )}
        }
    }

    @Test
    fun `En behandling som plutselig får melding på en annen personidentifikator`() = databaseTest { dataSource ->
        val hendelseId1 = HendelseId(UUID.randomUUID())
        val hendelseId2 = HendelseId(UUID.randomUUID())

        val behandlingFraHendelse1 = lagKomplettBehandling()
        val behandlingFraHendelse2 = behandlingFraHendelse1.copy()

        dataSource.connection.use { connection ->
            connection.finnRettHylle(
                hendelseId = hendelseId1,
                personidentifikator = Personidentifikator("11111111111"),
                behandling = behandlingFraHendelse1
            )

            assertThrows<NoSuchElementException> { connection.finnRettHylle(
                hendelseId = hendelseId2,
                personidentifikator = Personidentifikator("11111111112"),
                behandling = behandlingFraHendelse2
            )}
        }
    }

    @Test
    fun `Finne hyller på person + periode`() = databaseTest { dataSource ->
        val personidentifikator = Personidentifikator("12345678910")
        val opprettetBehandling = lagKomplettBehandling(fom = 2.januar, tom = 30.januar)

        dataSource.connection.use { connection ->
            connection.finnRettHylle(
                hendelseId = HendelseId(UUID.randomUUID()),
                personidentifikator = personidentifikator,
                behandling = opprettetBehandling
            )

            assertEquals(emptyList<FunnetHylle>(), connection.finnHyller(personidentifikator, Periode(1.januar, 1.januar))) // før
            assertEquals(opprettetBehandling, connection.finnHyller(personidentifikator, Periode(2.januar, 2.januar)).single().behandling) // snute
            assertEquals(opprettetBehandling, connection.finnHyller(personidentifikator, Periode(15.januar, 15.januar)).single().behandling) // mage
            assertEquals(opprettetBehandling, connection.finnHyller(personidentifikator, Periode(30.januar, 30.januar)).single().behandling) // hale
            assertEquals(emptyList<FunnetHylle>(), connection.finnHyller(personidentifikator, Periode(31.januar, 31.januar))) // etter
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
