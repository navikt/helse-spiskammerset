package no.nav.helse.spiskammerset.mottak

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.skyscreamer.jsonassert.JSONAssert
import java.util.*

@TestInstance(Lifecycle.PER_CLASS)
internal class LøsningContentEnricherRiverTest {
    private val rapid = TestRapid()

    private val sisteSendtMelding get() = rapid.inspektør.message(rapid.inspektør.size.minus(1))

    private val testSpiskammersetKlient = object : SpiskammersetKlient {
        override fun lagreLøsninger(packet: JsonMessage): Map<String, UUID> {
            return mapOf("SelvstendigForsikring" to UUID.fromString("00000000-0000-0000-0000-000000000000"))
        }
    }

    @BeforeAll
    fun setup() {
        rapid.apply {
            LøsningContentEnricherRiver(this, testSpiskammersetKlient)
        }
    }

    @BeforeEach
    fun beforeEach() {
        rapid.reset()
    }

    @Test
    fun `Lagrer og legger på id-er for komplette løsninger`() {
        @Language("JSON")
        val innkommendeMelding =
            """
            {
                "@event_name": "behov",
                "@behov": ["SelvstendigForsikring", "Medlemskap", "InntekterForOpptjening", "Arbeidsforhold"],
                "@id": "12345",
                "@opprettet": "2024-06-01T12:00:00",
                "fødselsnummer": "01010112345",
                "@final": true,
                "@lagreLøsninger": true,
                "@løsning": {
                  "SelvstendigForsikring": {
                    "forsikringstype": "ÅttiProsentFraDagEn",
                    "startdato": "2024-01-01",
                    "sluttdato": "2024-12-31"
                  },
                  "Medlemskap": {
                    "medlem": "VET IKKE"
                  },
                  "InntekterForOpptjening": {
                    "inntekter": [20000]
                  },
                  "Arbeidsforhold": {
                    "arbeidsforhold": [1]
                  }
                }
            }
            """

        rapid.sendTestMessage(innkommendeMelding)

        @Language("JSON")
        val forventetUtgåendeMelding =
            """
            {
                "@event_name": "behov",
                "@behov": ["SelvstendigForsikring", "Medlemskap", "InntekterForOpptjening", "Arbeidsforhold"],
                "fødselsnummer": "01010112345",
                "@final": true,
                "@lagreLøsninger": true,
                "@lagret": true,
                "@løsning": {
                  "SelvstendigForsikring": {
                    "forsikringstype": "ÅttiProsentFraDagEn",
                    "startdato": "2024-01-01",
                    "sluttdato": "2024-12-31",
                    "lagringId" : "urn:SelvstendigForsikring:00000000-0000-0000-0000-000000000000"
                  },
                  "Medlemskap": {
                    "medlem": "VET IKKE"
                  },
                  "InntekterForOpptjening": {
                    "inntekter": [20000]
                  },
                  "Arbeidsforhold": {
                    "arbeidsforhold": [1]
                  }
                }
            }
            """

        assertEquals(1, rapid.inspektør.size)
        val sisteSendteMelding = sisteSendtMelding as ObjectNode
        val meldingUtenRapidsAndRiversFelter = sisteSendteMelding.remove(setOf("@opprettet", "@id", "@forårsaket_av", "system_participating_services", "system_read_count"))

        JSONAssert.assertEquals(forventetUtgåendeMelding, meldingUtenRapidsAndRiversFelter.toString(), true)
    }
}