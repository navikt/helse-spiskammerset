package no.nav.helse.spiskammerset.oppbevaringsboks

import com.fasterxml.jackson.databind.node.ObjectNode

data class Hyllenummer(val nummer: Long)

data class Versjon(val nummer: Int) {
    init {
        check(nummer > 0)
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
