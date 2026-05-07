package no.nav.helse.spiskammerset.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import java.net.URI
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class FaktaIdTest {

    @Test
    fun `legger til faktaIndex og faktaId`() {
        @Language("JSON")
        val løsninger =
            """
            {
              "@løsning": {
                "SelvstendigForsikring": {
                  "fakta": [
                    {
                      "forsikringstype": "ÅttiProsentFraDagEn",
                      "startdato": "2024-01-01",
                      "sluttdato": "2024-12-31"
                    },
                    {
                      "forsikringstype": "EnAnnenForsikring",
                      "startdato": "2024-01-01",
                      "sluttdato": "2024-12-31"
                    }
                  ]
                },
                "Medlemskap": {
                  "medlem": "VET IKKE"
                },
                "NoeMedFaktaSomIkkeLAgres": {
                  "fakta": [
                    {
                      "boolean": true
                    },
                    {
                      "boolean": false
                    }
                  ]
                }
              }
            }
            """
        val packet = JsonMessage(løsninger, MessageProblems(løsninger)).also {
            it.requireKey("@løsning")
        }

        @Language("JSON")
        val forventetMedFaktaIndexer =
            """
            {
              "@løsning": {
                "SelvstendigForsikring": {
                  "fakta": [
                    {
                      "forsikringstype": "ÅttiProsentFraDagEn",
                      "startdato": "2024-01-01",
                      "sluttdato": "2024-12-31",
                      "@faktumIndex": 1
                    },
                    {
                      "forsikringstype": "EnAnnenForsikring",
                      "startdato": "2024-01-01",
                      "sluttdato": "2024-12-31",
                      "@faktumIndex": 2
                    }
                  ]
                },
                "Medlemskap": {
                  "medlem": "VET IKKE"
                },
                "NoeMedFaktaSomIkkeLAgres": {
                  "fakta": [
                    {
                      "boolean": true,
                      "@faktumIndex": 1

                    },
                    {
                      "boolean": false,
                      "@faktumIndex": 2
                    }
                  ]
                }
              }
            }
            """
        val faktiskMedFaktaIndexer = packet.medFaktumIndexer()

        assertJsonMessage(forventetMedFaktaIndexer, faktiskMedFaktaIndexer)

        @Language("JSON")
        val forventetMedFaktaIder =
            """
            {
              "@løsning": {
                "SelvstendigForsikring": {
                  "fakta": [
                    {
                      "forsikringstype": "ÅttiProsentFraDagEn",
                      "startdato": "2024-01-01",
                      "sluttdato": "2024-12-31",
                      "@faktumIndex": 1
                    },
                    {
                      "forsikringstype": "EnAnnenForsikring",
                      "startdato": "2024-01-01",
                      "sluttdato": "2024-12-31",
                      "@faktumIndex": 2
                    }
                  ],
                  "@faktaId": "urn:grunnlagsdata:forsikring:00000000-0000-0000-0000-000000000000"
                },
                "Medlemskap": {
                  "medlem": "VET IKKE"
                },
                "NoeMedFaktaSomIkkeLAgres": {
                  "fakta": [
                    {
                      "boolean": true

                    },
                    {
                      "boolean": false
                    }
                  ]
                }
              }
            }
            """
        val faktiskMedFaktaIder = packet.medFaktaIder(lagringIder = mapOf("SelvstendigForsikring" to URI("urn:grunnlagsdata:forsikring:00000000-0000-0000-0000-000000000000")))

        assertJsonMessage(forventetMedFaktaIder, faktiskMedFaktaIder)
    }
}
