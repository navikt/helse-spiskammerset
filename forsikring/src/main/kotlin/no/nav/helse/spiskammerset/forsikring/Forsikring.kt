package no.nav.helse.spiskammerset.forsikring

import no.nav.helse.spiskammerset.oppbevaringsboks.Versjon
import java.time.LocalDate

internal data class Forsikring(
    val forsikringstype: String,
    val premiegrunnlag: Int,
    val startdato: LocalDate,
    val sluttdato: LocalDate?,
    val versjon: Versjon
) {
    val innhold =
        mapOf(
            "premiegrunnlag" to premiegrunnlag
        ).plus(
            when (forsikringstype) {
                "ÅttiProsentFraDagEn" -> mapOf("dekningsgrad" to 80, "dag1Eller17" to 1)
                "HundreProsentFraDagEn" -> mapOf("dekningsgrad" to 100, "dag1Eller17" to 1)
                "HundreProsentFraDagEnJordbruker" -> mapOf("dekningsgrad" to 100, "dag1Eller17" to 1)
                "HundreProsentFraDagSytten" -> mapOf("dekningsgrad" to 100, "dag1Eller17" to 17)
                else -> error("Kjenner ikke til forsikringsype $forsikringstype")
            }
        )
}
