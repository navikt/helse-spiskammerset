package no.nav.helse.spiskammerset.spiskammerset

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.sql_dsl.*
import no.nav.helse.spiskammerset.oppbevaringsboks.Hyllenummer
import no.nav.helse.spiskammerset.spiskammerset.reisverk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.time.LocalDate
import java.util.*

class HendelseDaoTest {

    @Test
    fun `Håndtering av en hendelse`() = databaseTest { dataSource ->
        val hendelse = Hendelse(
            hendelseId = HendelseId(UUID.randomUUID()),
            hendelsetype = "behandling_opprettet",
            behandlinger = listOf(lagBehandling(1.januar, 31.januar), lagBehandling(1.februar, 28.februar)),
            json = objectmapper.readTree("""{"foo": true, "bar": 1}""") as ObjectNode
        )

        dataSource.connection.use { connection ->
            assertFalse(connection.håndtertTidligere(hendelse))
            val hyllenummer1 = connection.finnEllerOpprettHylle(hendelse.behandlinger[0]).hyllenummer
            val hyllenummer2 = connection.finnEllerOpprettHylle(hendelse.behandlinger[1]).hyllenummer
            connection.lagreHendelse(hendelse, setOf(hyllenummer1, hyllenummer2))
            assertTrue(connection.håndtertTidligere(hendelse))
            assertEquals(""""{\"foo\":true,\"bar\":1}"""", connection.hentJson(hendelse.hendelseId))
            val koblinger = connection.hentKoblinger()
            assertEquals(2, koblinger.size)
            assertEquals(koblinger[0].first, koblinger[1].first)
            assertEquals(setOf(hyllenummer1, hyllenummer2), koblinger.map { it.second }.toSet())
        }
    }


    private fun lagBehandling(fom: LocalDate, tom: LocalDate) = Behandling.KomplettBehandling(
        vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID()),
        behandlingId = BehandlingId(UUID.randomUUID()),
        periode = Periode(fom, tom),
        yrkesaktivitetstype = Yrkesaktivitetstype("SELVSTENDIG"),
        organisasjonsnummer = null,
        personidentifikator = Personidentifikator("11111111111")
    )

    private fun Connection.hentJson(hendelseId: HendelseId): String {
        val sql = "select hendelse from hendelse where hendelse_id = :hendelseId"
        return prepareStatementWithNamedParameters(sql) {
            withParameter("hendelseId", hendelseId.id)
        }.single { it.string("hendelse") }
    }

    private fun Connection.hentKoblinger(): List<Pair<Long, Hyllenummer>> {
        val sql = "select * from hendelser_paa_hylla"
        return prepareStatement(sql).mapNotNull { it.long("intern_hendelse_id") to Hyllenummer(it.long("hyllenummer")) }
    }

    private companion object {
        val objectmapper = jacksonObjectMapper()
    }
}
