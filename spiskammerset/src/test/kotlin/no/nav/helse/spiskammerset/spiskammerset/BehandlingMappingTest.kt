package no.nav.helse.spiskammerset.spiskammerset

import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import no.nav.helse.spiskammerset.oppbevaringsboks.Innhold
import no.nav.helse.spiskammerset.oppbevaringsboks.Versjon
import no.nav.helse.spiskammerset.spiskammerset.reisverk.BehandlingId
import no.nav.helse.spiskammerset.spiskammerset.reisverk.Hylle
import no.nav.helse.spiskammerset.spiskammerset.reisverk.Organisasjonsnummer
import no.nav.helse.spiskammerset.spiskammerset.reisverk.Periode
import no.nav.helse.spiskammerset.spiskammerset.reisverk.SvaretViGir
import no.nav.helse.spiskammerset.spiskammerset.reisverk.VedtaksperiodeId
import no.nav.helse.spiskammerset.spiskammerset.reisverk.Yrkesaktivitetstype
import no.nav.helse.spiskammerset.spiskammerset.reisverk.mapTilEndepunktformat
import no.nav.helse.spiskammerset.spiskammerset.reisverk.mapTilVedtaksperiodeformat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

class BehandlingMappingTest {

    @Test
    fun `én ting som mappes`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        val hyller =listOf(
            Hylle(
                vedtaksperiodeId = VedtaksperiodeId(vedtaksperiodeId),
                behandlingId = BehandlingId(behandlingId),
                periode = Periode(1.januar, 31.januar),
                yrkesaktivitetstype = Yrkesaktivitetstype("ARBEIDSTAKER"),
                organisasjonsnummer = Organisasjonsnummer("111111111"),
                opprettet = Testtidspunkt,
            ),
        )
        val actual = hyller.mapTilEndepunktformat()
        val expected = SvaretViGir(yrkesaktiviteter = listOf(SvaretViGir.Yrkesaktivitet(
            yrkesaktivitetstype = "ARBEIDSTAKER",
            organisasjonsnummer = "111111111",
            vedtaksperioder = listOf(SvaretViGir.Yrkesaktivitet.Vedtaksperioder(
                vedtaksperiodeId = vedtaksperiodeId,
                behandlinger = listOf(SvaretViGir.Yrkesaktivitet.Vedtaksperioder.Behandling(
                    behandlingId = behandlingId,
                    fom = 1.januar,
                    tom = 31.januar,
                    opprettet = Testtidspunkt
                ))
            ))
        )))

        assertEquals(expected, actual)
    }

    @Test
    fun `verifiser JSON output-format for hentAlt-endepunktet`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()

        val hylle = Hylle(
            vedtaksperiodeId = VedtaksperiodeId(vedtaksperiodeId),
            behandlingId = BehandlingId(behandlingId),
            periode = Periode(LocalDate.of(2025, 11, 1), LocalDate.of(2025, 11, 30)),
            yrkesaktivitetstype = Yrkesaktivitetstype("ARBEIDSTAKER"),
            organisasjonsnummer = Organisasjonsnummer("111111111"),
            opprettet = Testtidspunkt,
        )

        val forsikringInnhold = Innhold(
            versjon = Versjon(1),
            innhold = mapOf(
                "dekningsgrad" to 100,
                "dag1Eller17" to 1
            )
        )

        val behandlingerMedOppbevaringsbokser = listOf(
            hylle to mapOf("forsikring" to forsikringInnhold)
        )

        val result = behandlingerMedOppbevaringsbokser.mapTilVedtaksperiodeformat()

        val actualJson = objectmapper.writeValueAsString(result.vedtaksperioder[0])

        @Language("JSON")
        val expectedJson = """
        {
          "vedtaksperiodeId": "$vedtaksperiodeId",
          "periode": {
            "fom": "2025-11-01",
            "tom": "2025-11-30"
          },
          "behandlinger": [
            {
              "behandlingId": "$behandlingId",
              "forsikring": {
                "dekningsgrad": 100,
                "dag1Eller17": 1
              }
            }
          ]
        }
        """

        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT)
    }

}
