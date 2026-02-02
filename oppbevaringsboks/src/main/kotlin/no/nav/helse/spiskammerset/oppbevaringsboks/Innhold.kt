package no.nav.helse.spiskammerset.oppbevaringsboks

import com.fasterxml.jackson.databind.node.ObjectNode

data class Hyllenummer(val nummer: Long)

data class Versjon(val major: Int, val minor: Int, val patch: Int) {
    init {
        check(major >= 0)
        check(minor >= 0)
        check(patch > 0)
    }
    companion object {
        fun opprett(versjon: String) = versjon.split('.').let {
            Versjon(it[0].toInt(), it[1].toInt(), it[2].toInt())
        }
    }
}

class Innhold(
    val versjon: Versjon,
    val json: ObjectNode
)

sealed interface Innholdsstatus {
    data object EndretInnhold: Innholdsstatus
    data object UendretInnhold: Innholdsstatus
}
