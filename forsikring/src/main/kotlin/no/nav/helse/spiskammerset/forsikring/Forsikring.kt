package no.nav.helse.spiskammerset.forsikring

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.spiskammerset.oppbevaringsboks.Versjon

val mapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

data class Forsikring(val dekningsgrad: Int, val navOvertarAnsvarForVentetid: Boolean, val premiegrunnlag: Int, val versjon: Versjon) {
    internal fun tilJson() = mapper.readTree("""
        {
            "dekningsgrad": $dekningsgrad,
            "navOvertarAnsvarForVentetid": $navOvertarAnsvarForVentetid,
            "premiegrunnlag": $premiegrunnlag
        }
    """) as ObjectNode
}
