package no.nav.helse.spiskammerset.oppbevaringsboks

import com.fasterxml.jackson.databind.node.ObjectNode
import java.sql.Connection

interface Oppbevaringsboks {
    val etikett: String
    fun leggPå(hyllenummer: Hyllenummer, json: ObjectNode, connection: Connection): Innholdsstatus
    fun taNedFra(hyllenummer: Hyllenummer, connection: Connection): Innhold?
}
