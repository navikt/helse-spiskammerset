package no.nav.helse.spiskammerset.spiskammerset

import io.ktor.http.*
import no.nav.helse.spiskammerset.spiskammerset.reisverk.BehandlingId
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

internal class ApiTest : RestApiTest() {

    @Test
    fun `prøver å hente forsikring for behandling som ikke har forsikring`() = spiskammersetTestApp {
        val id = UUID.randomUUID()
        hentForsikring(
            behandlingId = BehandlingId(id),
            assertResponse = { status, responseBody ->
                assertEquals(HttpStatusCode.NotFound, status)
                assertJsonEquals("""{ "feilmelding": "Fant ikke forsikring for behandlingId: $id" }""", responseBody)
            }
        )
    }

    @Test
    fun `prøver å hente forsikring med feil rolle`() = spiskammersetTestApp {
        val id = UUID.randomUUID()
        hentForsikring(
            behandlingId = BehandlingId(id),
            accessToken = spiskammersetAccessToken("grevling"),
            assertResponse = { status, _ ->
                assertEquals(HttpStatusCode.Unauthorized, status)
            }
        )
    }

    @Test
    fun `henter ut forsikring`() = spiskammersetTestApp {
        val id = UUID.randomUUID()
        val hendelse = """ {
           "@event_name": "benyttet_grunnlagsdata_for_beregning",
            "@id": "${UUID.randomUUID()}",
            "fødselsnummer": "11111111111",
            "vedtaksperiodeId": "${UUID.randomUUID()}",
            "behandlingId": "$id",
            "fom": "2018-01-01",
            "tom": "2018-01-31",
            "yrkesaktivitetstype": "SELVSTENDIG",
            "forsikring": {
                "dekningsgrad": 100,
                "navOvertarAnsvarForVentetid": true,
                "premiegrunnlag": 500000
            }
        }
        """

        lagreHendelse(
            jsonBody = hendelse,
            assertResponse = { status, _ ->
                assertEquals(HttpStatusCode.NoContent, status)
            }
        )

        val forventetForsikring = """
            {
                "dekningsgrad": 100,
                "dag1Eller17": 1,
                "versjon": 1
            }
        """
        hentForsikring(
            behandlingId = BehandlingId(id),
            assertResponse = { status, response ->
                assertEquals(HttpStatusCode.OK, status)
                assertJsonEquals(forventetForsikring, response)
            }
        )
    }
}