package no.nav.helse.spiskammerset.spiskammerset.rest

import io.ktor.http.*
import no.nav.helse.spiskammerset.spiskammerset.reisverk.BehandlingId
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import no.nav.helse.spiskammerset.spiskammerset.Testtidspunkt

internal class OpplysningerApiTest : RestApiTest() {

    @Test
    fun `Hente ut kun fler opplysninger om gangen`() = restApiTest {
        val behandlingId = BehandlingId(UUID.randomUUID())

        lagreHendelse(
            jsonBody = testEvent("test_event_1", behandlingId, "en kjempekul verdi"),
            assertResponse = { status, _ ->
                assertEquals(HttpStatusCode.NoContent, status)
            }
        )

        // sender ikke test_event_2

        lagreHendelse(
            jsonBody = testEvent("test_event_3", behandlingId, "mjau"),
            assertResponse = { status, _ ->
                assertEquals(HttpStatusCode.NoContent, status)
            }
        )

        @Language("JSON")
        val forventetResponse = """
        {
            "info1": {
              "verdien": "en kjempekul verdi",
              "epoch": "1970-01-01T00:00:00",
              "versjon": 5
            },
            "info2": null,
            "info3": {
              "verdien": "mjau",
              "epoch": "1970-01-01T00:00:00",
              "versjon": 5
            }
        }
        """

        hentOpplysninger(
            behandlingId = behandlingId,
            opplysninger = setOf("info1", "info2", "info3"),
            assertResponse = { status, response ->
                assertEquals(HttpStatusCode.OK, status)
                assertJsonEquals(forventetResponse, response)
            }
        )
    }

    @Test
    fun `prøver å hente opplysning for behandling som ikke finnes`() = restApiTest {
        val behandlingId = BehandlingId(UUID.randomUUID())
        val callId = UUID.randomUUID()

        @Language("JSON")
        val forventetResponse = """
            {
              "type": "urn:error:not_found",
              "title": "Not Found",
              "status": 404,
              "detail": "Fant ikke info1 for behandlingId: $behandlingId",
              "instance": "/behandling/$behandlingId/info1",
              "callId": "$callId",
              "stacktrace": "Fant ikke info1 for behandlingId: $behandlingId"
            }
        """

        hentOpplysning(
            behandlingId = behandlingId,
            opplysning = "info1",
            callId = callId,
            assertResponse = { status, responseBody ->
                assertEquals(HttpStatusCode.NotFound, status)
                println(responseBody)
                assertJsonEquals(forventetResponse, responseBody)
            }
        )
    }

    @Test
    fun `prøver å hente opplysning for behandling som ikke har opplysninger om det`() = restApiTest {
        val behandlingId = BehandlingId(UUID.randomUUID())

        lagreHendelse(
            jsonBody = testEvent("test_event_1", behandlingId, null),
            assertResponse = { status, _ ->
                assertEquals(HttpStatusCode.NoContent, status)
            }
        )

        hentOpplysning(
            behandlingId = behandlingId,
            opplysning = "info2",
            assertResponse = { status, _ ->
                assertEquals(HttpStatusCode.NoContent, status)
            }
        )
    }

    @Test
    fun `prøver å hente opplysning med feil rolle`() = restApiTest {
        hentOpplysning(
            behandlingId = BehandlingId(UUID.randomUUID()),
            opplysning = "info3",
            accessToken = spiskammersetMaskinAccessToken("grevling"),
            assertResponse = { status, _ ->
                assertEquals(HttpStatusCode.Unauthorized, status)
            }
        )
    }

    @Test
    fun `en spissmus forrsøker å gjøre husmors arbeid`() = restApiTest {
        lagreHendelse(
            jsonBody = testEvent("test_event_1", BehandlingId(UUID.randomUUID()), null),
            accessToken = spiskammersetMaskinAccessToken("spissmus"),
            assertResponse = { status, _ ->
                assertEquals(HttpStatusCode.Unauthorized, status)
            }
        )
    }

    @Test
    fun `spissmus henter ut en opplysning`() = restApiTest {
        val behandlingId = BehandlingId(UUID.randomUUID())

        lagreHendelse(
            jsonBody = testEvent("test_event_2", behandlingId, "du skulle bare visst hvor kul verdi jeg er!"),
            assertResponse = { status, _ ->
                assertEquals(HttpStatusCode.NoContent, status)
            }
        )

        val forventetResponse = """
            {
                "verdien": "du skulle bare visst hvor kul verdi jeg er!",
                "epoch": "1970-01-01T00:00:00",
                "versjon": 5
            }
        """
        hentOpplysning(
            behandlingId = behandlingId,
            opplysning = "info2",
            assertResponse = { status, response ->
                assertEquals(HttpStatusCode.OK, status)
                assertJsonEquals(forventetResponse, response)
            }
        )
    }

    @Test
    fun `en av spissmusens hjelpere henter ut en opplysning`() = restApiTest {
        val behandlingId = BehandlingId(UUID.randomUUID())

        lagreHendelse(
            jsonBody = testEvent("test_event_2", behandlingId, "du skulle bare visst hvor kul verdi jeg er!"),
            assertResponse = { status, _ ->
                assertEquals(HttpStatusCode.NoContent, status)
            }
        )

        @Language("JSON")
        val forventetResponse = """
            {
                "verdien": "du skulle bare visst hvor kul verdi jeg er!",
                "epoch": "1970-01-01T00:00:00",
                "versjon": 5
            }
        """
        hentOpplysning(
            behandlingId = behandlingId,
            accessToken = spiskammersetPersonToken(),
            opplysning = "info2",
            assertResponse = { status, response ->
                assertEquals(HttpStatusCode.OK, status)
                assertJsonEquals(forventetResponse, response)
            }
        )
    }

    @Language("JSON")
    private fun testEvent(eventName: String, behandlingId: BehandlingId, verdien: String?) = """ {
        "@event_name": "$eventName",
        "@id": "${UUID.randomUUID()}",
        "fødselsnummer": "11111111111",
        "vedtaksperiodeId": "${UUID.randomUUID()}",
        "behandlingId": "${behandlingId.id}",
        "fom": "2018-01-01",
        "tom": "2018-01-31",
        "yrkesaktivitetstype": "SELVSTENDIG",
        ${if (verdien != null) """"verdien": "$verdien",""" else ""}
        "behandlingOpprettet": "$Testtidspunkt"
    }
    """
}
