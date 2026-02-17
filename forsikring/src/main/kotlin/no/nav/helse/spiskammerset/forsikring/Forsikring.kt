package no.nav.helse.spiskammerset.forsikring

import no.nav.helse.spiskammerset.oppbevaringsboks.Versjon

internal data class Forsikring(val dekningsgrad: Int, val navOvertarAnsvarForVentetid: Boolean, val premiegrunnlag: Int, val arbeidssituasjonForsikringstype: ArbeidssituasjonForsikringstype, val versjon: Versjon) {
    init {
        when (arbeidssituasjonForsikringstype) {
            ArbeidssituasjonForsikringstype.KollektivJordbruksforsikring -> {
                require(premiegrunnlag == 0) { "Premiegrunnlag for KollektivJordbruksforsikring må være 0, premiegrunnlaget var $premiegrunnlag" }
                require(!navOvertarAnsvarForVentetid) { "NAV overtar ikke ansvar for ventetid ved KollektivJordbruksforsikring" }
                require(dekningsgrad == 100) { "Dekningsgrad for KollektivJordbruksforsikring må være 100 prosent, dekningsgraden var: $dekningsgrad" }
            }
            ArbeidssituasjonForsikringstype.SelvstendigForsikring -> require(premiegrunnlag > 0) { "Premiegrunnlaget for SelvstendigForsikring må være større enn 0, premiegrunnlaget var $premiegrunnlag" }
        }
    }

    val innhold = mapOf(
        "dekningsgrad" to dekningsgrad,
        "dag1Eller17" to when (navOvertarAnsvarForVentetid) {
            true -> 1
            false -> 17
        }
    )
    enum class ArbeidssituasjonForsikringstype {
        SelvstendigForsikring,
        KollektivJordbruksforsikring
    }
}
