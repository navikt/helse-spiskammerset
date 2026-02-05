package no.nav.helse.spiskammerset.forsikring

import no.nav.helse.spiskammerset.oppbevaringsboks.Versjon

internal data class Forsikring(val dekningsgrad: Int, val navOvertarAnsvarForVentetid: Boolean, val premiegrunnlag: Int, val versjon: Versjon) {
    val innhold = mapOf(
        "dekningsgrad" to dekningsgrad,
        "dag1Eller17" to when (navOvertarAnsvarForVentetid) {
            true -> 1
            false -> 17
        }
    )
}
