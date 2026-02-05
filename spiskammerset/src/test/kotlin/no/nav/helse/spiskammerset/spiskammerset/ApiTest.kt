package no.nav.helse.spiskammerset.spiskammerset

import io.ktor.http.*
import no.nav.helse.spiskammerset.spiskammerset.reisverk.BehandlingId
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.assertEquals

internal class ApiTest : RestApiTest() {

    @Test
    fun `prøver å hente forsikring for behandling som ikke finnes`() = spiskammersetTestApp {
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
    fun `prøver å hente forsikring for behandling som ikke har forsikring`() = spiskammersetTestApp {
        val id = UUID.randomUUID()

        lagreHendelse(
            jsonBody = benyttetGrunnlagsdataForBeregning(id, "mjau"),
            assertResponse = { status, _ ->
                assertEquals(HttpStatusCode.NoContent, status)
            }
        )

        hentForsikring(
            behandlingId = BehandlingId(id),
            assertResponse = { status, _ ->
                assertEquals(HttpStatusCode.NoContent, status)
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

        lagreHendelse(
            jsonBody = benyttetGrunnlagsdataForBeregning(id),
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

    @Language("JSON")
    private fun benyttetGrunnlagsdataForBeregning(behandlingId: UUID, forsikringKey: String = "forsikring") = """ {
       "@event_name": "benyttet_grunnlagsdata_for_beregning",
        "@id": "${UUID.randomUUID()}",
        "fødselsnummer": "11111111111",
        "vedtaksperiodeId": "${UUID.randomUUID()}",
        "behandlingId": "${behandlingId}",
        "fom": "2018-01-01",
        "tom": "2018-01-31",
        "yrkesaktivitetstype": "SELVSTENDIG",
        "$forsikringKey": {
            "dekningsgrad": 100,
            "navOvertarAnsvarForVentetid": true,
            "premiegrunnlag": 500000
        },
        "behandlingOpprettet": "${OffsetDateTime.now()}"
    }
    """
}
