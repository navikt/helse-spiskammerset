package no.nav.helse.spiskammerset.oppbevaringsboks

import com.fasterxml.jackson.databind.node.ObjectNode
import java.sql.Connection
import java.util.*

interface Oppbevaringsboks {
    val etikett: String
    @Deprecated("Bruk puttI") fun leggPå(hyllenummer: Hyllenummer, json: ObjectNode, connection: Connection): Innholdsstatus = error("Ikke implementert")
    @Deprecated("Bruk taUt") fun taNedFra(hyllenummer: Hyllenummer, connection: Connection): Innhold? = error("Ikke implementert")
    // TODO disse to skal erstatte leggPå og taNedFra
    fun puttI(json: ObjectNode, connection: Connection): UUID = error("puttI er ikke implementert")
    fun taUt(id: UUID, connection: Connection): Innhold? = error("taUt er ikke implementert")
}
