package no.nav.helse.spiskammerset.spiskammerset.rest

import io.ktor.http.*
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

internal class LøsningerApiTest : RestApiTest(oppbevaringsbokser = listOf(
    BehovLøsningTestOppbevaringsboks(setOf("TøyseteBehov1"), "tøysete-behov-1", UUID.fromString("00000000-0000-0000-0000-000000000001")),
    BehovLøsningTestOppbevaringsboks(setOf("TøyseteBehov3"), "tøysete-behov-3", UUID.fromString("00000000-0000-0000-0000-000000000003")),
    BehovLøsningTestOppbevaringsboks(setOf("Faktating", "FaktatingV2"), "faktating", UUID.fromString("00000000-0000-0000-0000-000000000004"))

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
              },
              "TøyseteBehov1": {},
              "TøyseteBehov2": {},
              "TøyseteBehov3": {}
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
                        "tøysete-behov-1": "00000000-0000-0000-0000-000000000001",
                        "tøysete-behov-3": "00000000-0000-0000-0000-000000000003"
                      }
                    }
                    """
                assertJsonEquals(forventetResponse, responseBody)
            }
        )

        hentLøsning(
            lagringId = "urn:grunnlagsdata:tøysete-behov-1:00000000-0000-0000-0000-000000000001",
            assertResponse = { status, responseBody ->
                assertEquals(HttpStatusCode.OK, status)
                @Language("JSON")
                val forventetResponse =
                    """
                    {
                      "innhold": "Kult innhold",
                      "versjon": 5,
                      "epoch": "1970-01-01T00:00:00"
                    }
                    """
                assertJsonEquals(forventetResponse, responseBody)
            }
        )

        hentLøsning(
            lagringId = "urn:grunnlagsdata:tøysete-behov-2:00000000-0000-0000-0000-000000000002",
            assertResponse = { status, _ ->
                assertEquals(HttpStatusCode.NotFound, status)
            }
        )

        hentLøsning(
            lagringId = "urn:grunnlagsdata:tøysete-behov-3:00000000-0000-0000-0000-000000000003",
            assertResponse = { status, responseBody ->
                assertEquals(HttpStatusCode.OK, status)
                @Language("JSON")
                val forventetResponse =
                    """
                    {
                      "innhold": "3",
                      "versjon": 5,
                      "epoch": "1970-01-01T00:00:00"
                    }
                    """
                assertJsonEquals(forventetResponse, responseBody)
            }
        )
    }

    @Test
    fun `Flere behov som hører til samme oppbevaringsboks`() = restApiTest {
        @Language("JSON")
        val innkommendeMelding =
            """
        {
          "@event_name": "behov",
          "@behov": ["Faktating", "FaktatingV2"],
          "fødselsnummer": "01010112345",
          "@final": true,
          "@lagreLøsninger": true,
          "@løsning": {
            "Faktating": {
              "innhold": "Jeg er det gamle behovet"
            },
            "FaktatingV2": {
              "innhold": "Og jeg er det nye"
            }
          },
          "Faktating": {},
          "FaktatingV2": {}
        }
        """

        lagreLøsninger(
            jsonBody = innkommendeMelding,
            assertResponse = { status, _ ->
                assertEquals(HttpStatusCode.InternalServerError, status)
            }
        )
    }

    @Test
    fun `Flere behov som hører til samme oppbevaringsboks, men et skal ignoreres`() = restApiTest {
        @Language("JSON")
        val innkommendeMelding =
            """
            {
              "@event_name": "behov",
              "@behov": ["Faktating", "FaktatingV2"],
              "fødselsnummer": "01010112345",
              "@final": true,
              "@lagreLøsninger": true,
              "@løsning": {
                "Faktating": {
                  "innhold": "Jeg er det gamle behovet"
                },
                "FaktatingV2": {
                  "innhold": "Og jeg er det nye"
                }
              },
              "Faktating": {},
              "FaktatingV2": {
                "ignorer": true
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
                        "faktating": "00000000-0000-0000-0000-000000000004"
                      }
                    }
                    """
                assertJsonEquals(forventetResponse, responseBody)
            }
        )

        hentLøsning(
            lagringId = "urn:grunnlagsdata:faktating:00000000-0000-0000-0000-000000000004",
            assertResponse = { status, responseBody ->
                assertEquals(HttpStatusCode.OK, status)
                @Language("JSON")
                val forventetResponse =
                    """
                    {
                      "innhold": "Jeg er det gamle behovet",
                      "versjon": 5,
                      "epoch": "1970-01-01T00:00:00"
                    }
                    """
                assertJsonEquals(forventetResponse, responseBody)
            }
        )
    }
}
