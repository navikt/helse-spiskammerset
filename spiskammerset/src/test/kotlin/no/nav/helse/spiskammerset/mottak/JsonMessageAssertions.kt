package no.nav.helse.spiskammerset.mottak

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import org.skyscreamer.jsonassert.JSONAssert

fun assertJsonMessage(forventet: String, faktisk: JsonNode) {
    check(faktisk is ObjectNode) { "Forventet er ikke JSON-object"}
    val meldingUtenRapidsAndRiversFelter = faktisk.remove(setOf("@opprettet", "@id", "@forårsaket_av", "system_participating_services", "system_read_count"))
    JSONAssert.assertEquals(forventet, meldingUtenRapidsAndRiversFelter.toString(), true)
}

fun assertJsonMessage(forventet: String, faktisk: String) {
    assertJsonMessage(forventet, jacksonObjectMapper().readTree(faktisk) as ObjectNode)
}

fun assertJsonMessage(forventet: String, faktisk: JsonMessage) {
    assertJsonMessage(forventet, faktisk.toJson())
}
