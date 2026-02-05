package no.nav.helse.spiskammerset.spiskammerset.rest

import io.ktor.http.HttpStatusCode
import no.nav.helse.spiskammerset.spiskammerset.reisverk.BehandlingId
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals

internal class OpplysningerApiTest : RestApiTest() {

    @Test
    fun `Hente ut kun opplysninger man spør om`() = restApiTest {
        val id = UUID.randomUUID()

        lagreHendelse(
            jsonBody = benyttetGrunnlagsdataForBeregning(id),
            assertResponse = { status, _ ->
                assertEquals(HttpStatusCode.Companion.NoContent, status)
            }
        )

        @Language("JSON")
        val forventetForsikring = """
        {
              "forsikring": {
                "dekningsgrad": 100,
                "dag1Eller17": 1,
                "versjon": 1
            }
        }
        """

        hentOpplysninger(
            behandlingId = BehandlingId(id),
            opplysninger = setOf("forsikring"),
            assertResponse = { status, response ->
                assertEquals(HttpStatusCode.Companion.OK, status)
                assertJsonEquals(forventetForsikring, response)
            }
        )
    }

    @Test
    fun `prøver å hente forsikring for behandling som ikke finnes`() = restApiTest {
        val id = UUID.randomUUID()
        hentForsikring(
            behandlingId = BehandlingId(id),
            assertResponse = { status, responseBody ->
                assertEquals(HttpStatusCode.Companion.NotFound, status)
                assertJsonEquals("""{ "feilmelding": "Fant ikke forsikring for behandlingId: $id" }""", responseBody)
            }
        )
    }

    @Test
    fun `prøver å hente forsikring for behandling som ikke har forsikring`() = restApiTest {
        val id = UUID.randomUUID()

        lagreHendelse(
            jsonBody = benyttetGrunnlagsdataForBeregning(id, "mjau"),
            assertResponse = { status, _ ->
                assertEquals(HttpStatusCode.Companion.NoContent, status)
            }
        )

        hentForsikring(
            behandlingId = BehandlingId(id),
            assertResponse = { status, _ ->
                assertEquals(HttpStatusCode.Companion.NoContent, status)
            }
        )
    }


    @Test
    fun `prøver å hente forsikring med feil rolle`() = restApiTest {
        val id = UUID.randomUUID()
        hentForsikring(
            behandlingId = BehandlingId(id),
            accessToken = spiskammersetAccessToken("grevling"),
            assertResponse = { status, _ ->
                assertEquals(HttpStatusCode.Companion.Unauthorized, status)
            }
        )
    }

    @Test
    fun `henter ut forsikring`() = restApiTest {
        val id = UUID.randomUUID()

        lagreHendelse(
            jsonBody = benyttetGrunnlagsdataForBeregning(id),
            assertResponse = { status, _ ->
                assertEquals(HttpStatusCode.Companion.NoContent, status)
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
                assertEquals(HttpStatusCode.Companion.OK, status)
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
