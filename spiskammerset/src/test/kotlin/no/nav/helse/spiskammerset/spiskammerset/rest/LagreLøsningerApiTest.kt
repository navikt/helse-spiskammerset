package no.nav.helse.spiskammerset.spiskammerset.rest

import io.ktor.http.*
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

internal class LagreLøsningerApiTest : RestApiTest(oppbevaringsbokser = listOf(
    BehovLøsningTestOppbevaringsboks("TøyseteBehov1", UUID.fromString("00000000-0000-0000-0000-000000000001")),
    BehovLøsningTestOppbevaringsboks("TøyseteBehov3", UUID.fromString("00000000-0000-0000-0000-000000000003")),
)) {

    @Test
    fun `Lagrer løsninger på behov som det er snekret oppbevaringsbokser for`() = restApiTest {
        @Language("JSON")
        val innkommendeMelding =
            """
            {
                "@event_name": "behov",
                "@behov": ["TøyseteBehov1", "TøyseteBehov2", "TøyseteBehov3"],
                "@id": "12345",
                "@opprettet": "2024-06-01T12:00:00",
                "fødselsnummer": "01010112345",
                "@final": true,
                "@lagreLøsninger": true,
                "@løsning": {
                  "TøyseteBehov1": {
                    "innhold": "Kult innhold"
                  },
                  "TøyseteBehov2": {
                    "innhold": true
                  },
                  "TøyseteBehov3": {
                    "innhold": 3
                  }
                }
            }
            """

        lagreLøsninger(
            jsonBody = innkommendeMelding,
            assertResponse = { status, responseBody ->
                assertEquals(HttpStatusCode.OK, status)
                @Language("JSON")
                val forventetResponse =
                    """
                    {
                      "lagredeLøsningIder": {
                        "TøyseteBehov1": "00000000-0000-0000-0000-000000000001",
                        "TøyseteBehov3": "00000000-0000-0000-0000-000000000003"
                      }
                    }
                    """
                assertJsonEquals(forventetResponse, responseBody)
            }
        )

        // TODO hente innholdet fra boksen fra id
    }
}