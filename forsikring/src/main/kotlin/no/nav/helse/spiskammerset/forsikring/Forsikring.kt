package no.nav.helse.spiskammerset.forsikring

import com.fasterxml.jackson.databind.node.ObjectNode
import java.sql.Connection
import no.nav.helse.spiskammerset.oppbevaringsboks.Hyllenummer
import no.nav.helse.spiskammerset.oppbevaringsboks.Innhold
import no.nav.helse.spiskammerset.oppbevaringsboks.Innholdsstatus
import no.nav.helse.spiskammerset.oppbevaringsboks.Oppbevaringsboks

data object Forsikring: Oppbevaringsboks {
    override val etikett = "forsikring"

    override fun leggPå(hyllenummer: Hyllenummer, json: ObjectNode, connection: Connection): Innholdsstatus {
        TODO("Not yet implemented")
    }

    override fun taNedFra(hyllenummer: Hyllenummer, connection: Connection): Innhold? {
        TODO("Not yet implemented")
    }
}
