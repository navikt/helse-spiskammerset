package no.nav.helse.spiskammerset.spiskammerset

import java.util.UUID
import kotlin.test.assertEquals
import no.nav.helse.spiskammerset.spiskammerset.reisverk.BehandlingId
import no.nav.helse.spiskammerset.spiskammerset.reisverk.Hylle
import no.nav.helse.spiskammerset.spiskammerset.reisverk.Organisasjonsnummer
import no.nav.helse.spiskammerset.spiskammerset.reisverk.Periode
import no.nav.helse.spiskammerset.spiskammerset.reisverk.SvaretViGir
import no.nav.helse.spiskammerset.spiskammerset.reisverk.VedtaksperiodeId
import no.nav.helse.spiskammerset.spiskammerset.reisverk.Yrkesaktivitetstype
import no.nav.helse.spiskammerset.spiskammerset.reisverk.mapTilEndepunktformat
import org.junit.jupiter.api.Test

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

}
