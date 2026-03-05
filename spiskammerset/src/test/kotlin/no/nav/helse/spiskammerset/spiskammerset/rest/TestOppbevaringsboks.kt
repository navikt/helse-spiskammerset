package no.nav.helse.spiskammerset.spiskammerset.rest

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.spiskammerset.oppbevaringsboks.*
import java.sql.Connection
import java.time.LocalDate
import java.util.*

data class TestOppbevaringsboks(private val eventName: String, override val etikett: String): Oppbevaringsboks {
    private var verdien: String? = null

    override fun leggPå(hyllenummer: Hyllenummer, json: ObjectNode, connection: Connection): Innholdsstatus {
        if (json.path("@event_name").asText() != eventName) return Innholdsstatus.UendretInnhold
        verdien = json.path("verdien").asText()
        return Innholdsstatus.EndretInnhold
    }

    override fun taNedFra(hyllenummer: Hyllenummer, connection: Connection) = when (verdien) {
        null -> null
        else -> Innhold(Versjon(5), mapOf(
            "verdien" to verdien,
            "epoch" to LocalDate.EPOCH.atStartOfDay()
        ))
    }
}

data class BehovLøsningTestOppbevaringsboks(private val løsningsnavn: String, private val id: UUID) : Oppbevaringsboks {
    override val etikett = løsningsnavn
    private var verdien: String? = null

    override fun puttI(json: ObjectNode, connection: Connection): UUID {
        verdien = json.path("@løsning").path(løsningsnavn).path("verdien").asText()
        return id
    }

    override fun taUt(id: UUID, connection: Connection): Innhold? {
        if (this.id != id) return null
        return when (verdien) {
            null -> null
            else -> Innhold(
                Versjon(5), mapOf(
                    "verdien" to verdien,
                    "epoch" to LocalDate.EPOCH.atStartOfDay()
                )
            )
        }
    }
}
