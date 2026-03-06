package no.nav.helse.spiskammerset.spiskammerset.rest

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.spiskammerset.oppbevaringsboks.Innhold
import no.nav.helse.spiskammerset.oppbevaringsboks.Oppbevaringsboks
import no.nav.helse.spiskammerset.oppbevaringsboks.Versjon
import java.sql.Connection
import java.time.LocalDate
import java.util.*

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
