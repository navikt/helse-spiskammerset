package no.nav.helse.spiskammerset.spiskammerset.rest

import com.fasterxml.jackson.databind.node.ObjectNode
import java.sql.Connection
import java.time.LocalDate
import no.nav.helse.spiskammerset.oppbevaringsboks.Hyllenummer
import no.nav.helse.spiskammerset.oppbevaringsboks.Innhold
import no.nav.helse.spiskammerset.oppbevaringsboks.Innholdsstatus
import no.nav.helse.spiskammerset.oppbevaringsboks.Oppbevaringsboks
import no.nav.helse.spiskammerset.oppbevaringsboks.Versjon

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
