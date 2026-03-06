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

data class BehovLøsningTestOppbevaringsboks(
    override val behovsnavn: Set<String>,
    override val etikett: String,
    private val id: UUID
) : Oppbevaringsboks {
    private var innhold: String? = null

    override fun puttI(json: ObjectNode, connection: Connection): UUID {
        innhold = json.path("innhold").asText()
        return id
    }

    override fun taUt(id: UUID, connection: Connection): Innhold? {
        if (this.id != id) return null
        return when (innhold) {
            null -> null
            else -> Innhold(
                Versjon(5), mapOf(
                    "innhold" to innhold,
                    "epoch" to LocalDate.EPOCH.atStartOfDay()
                )
            )
        }
    }
}
