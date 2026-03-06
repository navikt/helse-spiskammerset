package no.nav.helse.spiskammerset.oppbevaringsboks

import com.fasterxml.jackson.databind.node.ObjectNode
import java.sql.Connection
import java.util.*

interface Oppbevaringsboks {
    val etikett: String
    val behovsnavn: Set<String>
    fun puttI(json: ObjectNode, connection: Connection): UUID
    fun taUt(id: UUID, connection: Connection): Innhold?

    companion object {
        fun List<Oppbevaringsboks>.valider() {
            val etiketter = map { it.etikett }
            require(etiketter.size == etiketter.toSet().size) { "Det er flere oppbevaringsbokser med samme etiketter!!" }
            val behov = flatMap { it.behovsnavn }
            require(behov.size == behov.toSet().size) { "Det er flere oppbevaringsbokser som håndterer samme behov!!" }
        }
    }
}
