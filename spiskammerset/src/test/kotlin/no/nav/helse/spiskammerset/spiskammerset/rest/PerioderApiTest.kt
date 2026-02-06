package no.nav.helse.spiskammerset.spiskammerset.rest

import io.ktor.http.*
import no.nav.helse.spiskammerset.spiskammerset.april
import no.nav.helse.spiskammerset.spiskammerset.januar
import no.nav.helse.spiskammerset.spiskammerset.mars
import no.nav.helse.spiskammerset.spiskammerset.reisverk.*
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import no.nav.helse.spiskammerset.spiskammerset.Testtidspunkt

internal class PerioderApiTest : RestApiTest() {

    @Test
    fun `henter ut perioder`() = restApiTest {
        val behandlingId1 = BehandlingId(UUID.randomUUID())
        val behandlingId2 = BehandlingId(UUID.randomUUID())
        val behandlingId3 = BehandlingId(UUID.randomUUID())
        val behandlingId4 = BehandlingId(UUID.randomUUID())
        val behandlingId5 = BehandlingId(UUID.randomUUID())
        val vedtaksperiodeId1 = VedtaksperiodeId(UUID.randomUUID())
        val vedtaksperiodeId2 = VedtaksperiodeId(UUID.randomUUID())
        val vedtaksperiodeId3 = VedtaksperiodeId(UUID.randomUUID())
        val vedtaksperiodeId4 = VedtaksperiodeId(UUID.randomUUID())
        lagreHendelse(jsonBody = behandlingOpprettetEvent(behandlingId1, vedtaksperiodeId1, Periode(10.januar, 31.januar), Yrkesaktivitetstype("SELVSTENDIG")))
        lagreHendelse(jsonBody = behandlingOpprettetEvent(behandlingId2, vedtaksperiodeId1, Periode(1.januar, 31.januar), Yrkesaktivitetstype("SELVSTENDIG")))
        lagreHendelse(jsonBody = behandlingOpprettetEvent(behandlingId3, vedtaksperiodeId2, Periode(1.mars, 31.mars), Yrkesaktivitetstype("FRILANSER")))
        lagreHendelse(jsonBody = behandlingOpprettetEvent(behandlingId4, vedtaksperiodeId3, Periode(1.april, 30.april), Yrkesaktivitetstype("FRILANSER")))
        lagreHendelse(jsonBody = behandlingOpprettetEvent(behandlingId5, vedtaksperiodeId4, Periode(1.april, 30.april), Yrkesaktivitetstype("ARBEIDSTAKER"), organisasjonsnummer = Organisasjonsnummer("999999999")))

        @Language("JSON")
        val forventetResponse = """
        {
          "yrkesaktiviteter": [
            {
              "yrkesaktivitetstype": "SELVSTENDIG",
              "vedtaksperioder": [
                {
                  "vedtaksperiodeId": "$vedtaksperiodeId1",
                  "behandlinger": [
                    {
                      "behandlingId": "$behandlingId1",
                      "fom": "2018-01-10",
                      "tom": "2018-01-31",
                      "opprettet": "2026-02-06T14:00:00.627409Z"
                    },
                    {
                      "behandlingId": "$behandlingId2",
                      "fom": "2018-01-01",
                      "tom": "2018-01-31",
                      "opprettet": "2026-02-06T14:00:00.627409Z"
                    }
                  ]
                }
              ]
            },
            {
              "yrkesaktivitetstype": "FRILANSER",
              "vedtaksperioder": [
                {
                  "vedtaksperiodeId": "$vedtaksperiodeId2",
                  "behandlinger": [
                    {
                      "behandlingId": "$behandlingId3",
                      "fom": "2018-03-01",
                      "tom": "2018-03-31",
                      "opprettet": "2026-02-06T14:00:00.627409Z"
                    }
                  ]
                },
                {
                  "vedtaksperiodeId": "$vedtaksperiodeId3",
                  "behandlinger": [
                    {
                      "behandlingId": "$behandlingId4",
                      "fom": "2018-04-01",
                      "tom": "2018-04-30",
                      "opprettet": "2026-02-06T14:00:00.627409Z"
                    }
                  ]
                }
              ]
            },
            {
              "yrkesaktivitetstype": "ARBEIDSTAKER",
              "organisasjonsnummer": "999999999",
              "vedtaksperioder": [
                {
                  "vedtaksperiodeId": "$vedtaksperiodeId4",
                  "behandlinger": [
                    {
                      "behandlingId": "$behandlingId5",
                      "fom": "2018-04-01",
                      "tom": "2018-04-30",
                      "opprettet": "2026-02-06T14:00:00.627409Z"
                    }
                  ]
                }
              ]
            }
          ]
        }
        """
        hentPerioder(
            personidentifikatorer = listOf("11111111111"),
            fom = 1.januar,
            tom = 30.april,
            assertResponse = { status, responseBody ->
                assertEquals(HttpStatusCode.OK, status)
                assertJsonEquals(forventetResponse, responseBody)
            }
        )
    }

    @Language("JSON")
    private fun behandlingOpprettetEvent(behandlingId: BehandlingId, vedtaksperiodeId: VedtaksperiodeId, periode: Periode, yrkesaktivitetstype: Yrkesaktivitetstype, organisasjonsnummer: Organisasjonsnummer? = null) = """ {
        "@event_name": "behandling_opprettet",
        "@id": "${UUID.randomUUID()}",
        "fødselsnummer": "11111111111",
        "vedtaksperiodeId": "${vedtaksperiodeId.id}",
        "behandlingId": "${behandlingId.id}",
        "fom": "${periode.fom}",
        "tom": "${periode.tom}",
        "yrkesaktivitetstype": "${yrkesaktivitetstype.type}",
        ${if (organisasjonsnummer != null) """"organisasjonsnummer": "$organisasjonsnummer",""" else ""}
        "behandlingOpprettet": "$Testtidspunkt"
    }
    """
}
