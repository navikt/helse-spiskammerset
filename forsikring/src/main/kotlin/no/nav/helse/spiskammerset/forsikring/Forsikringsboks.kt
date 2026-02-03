package no.nav.helse.spiskammerset.forsikring

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.spiskammerset.oppbevaringsboks.*
import java.sql.Connection

data object Forsikringsboks: Oppbevaringsboks {
    override val etikett = "forsikring"

    override fun leggPå(hyllenummer: Hyllenummer, json: ObjectNode, connection: Connection): Innholdsstatus {
        val dao = ForsikringDao(connection)
        val forsikring = Forsikring(
            dekningsgrad = json.path("dekningsgrad").asInt(),
            navOvertarAnsvarForVentetid = json.path("navOvertarAnsvarForVentetid").asBoolean(),
            premiegrunnlag = json.path("premiegrunnlag").asInt()
        )

        return when (dao.lagre(forsikring, hyllenummer)) {
            0 -> Innholdsstatus.UendretInnhold
            else -> Innholdsstatus.EndretInnhold
        }
    }

    override fun taNedFra(hyllenummer: Hyllenummer, connection: Connection): Innhold? {
        val dao = ForsikringDao(connection)
        val forsikring = dao.hent(hyllenummer)

        return when (forsikring) {
            null -> null
            else -> Innhold(Versjon(0, 0, 0), forsikring.tilJson()) // TODO hva er versjonsnummer??
        }
    }
}
