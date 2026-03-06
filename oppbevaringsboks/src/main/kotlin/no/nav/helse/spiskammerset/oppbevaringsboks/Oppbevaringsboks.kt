package no.nav.helse.spiskammerset.oppbevaringsboks

import com.fasterxml.jackson.databind.node.ObjectNode
import java.sql.Connection
import java.util.*

interface Oppbevaringsboks {
    // TODO skal klassen definere en generic id-klasse?
    val etikett: String
    val behovsnavn: Set<String> get() = emptySet()
    @Deprecated("Bruk puttI") fun leggPå(hyllenummer: Hyllenummer, json: ObjectNode, connection: Connection): Innholdsstatus = error("Ikke implementert")
    @Deprecated("Bruk taUt") fun taNedFra(hyllenummer: Hyllenummer, connection: Connection): Innhold? = error("Ikke implementert")
    // TODO disse to skal erstatte leggPå og taNedFra
    fun puttI(json: ObjectNode, connection: Connection): UUID = error("puttI er ikke implementert")
    fun taUt(id: String, connection: Connection): Innhold? = error("taUt er ikke implementert")

    companion object {
        fun List<Oppbevaringsboks>.valider() {
            val etiketter = map { it.etikett }
            require(etiketter.size == etiketter.toSet().size) { "Det er flere oppbevaringsbokser med samme etiketter!!" }
            val behov = flatMap { it.behovsnavn }
            require(behov.size == behov.toSet().size) { "Det er flere oppbevaringsbokser som håndterer samme behov!!" }
        }
    }
}
