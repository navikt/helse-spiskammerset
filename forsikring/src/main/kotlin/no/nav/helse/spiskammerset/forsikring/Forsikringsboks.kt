package no.nav.helse.spiskammerset.forsikring

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.spiskammerset.oppbevaringsboks.*
import java.sql.Connection

data object Forsikringsboks: Oppbevaringsboks {
    override val etikett = "forsikring"
    private val gjeldendeVersjon = Versjon(1)

    override fun leggPå(hyllenummer: Hyllenummer, json: ObjectNode, connection: Connection): Innholdsstatus {
        val forsikringJson = json["forsikring"] ?: return Innholdsstatus.UendretInnhold
        val dao = ForsikringDao(connection)
        val forsikring = Forsikring(
            dekningsgrad = forsikringJson["dekningsgrad"].asInt(),
            navOvertarAnsvarForVentetid = forsikringJson["navOvertarAnsvarForVentetid"].asBoolean(),
            premiegrunnlag = forsikringJson["premiegrunnlag"].asInt(),
            versjon = gjeldendeVersjon
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
            else -> Innhold(forsikring.versjon, forsikring.tilJson()) // TODO hva er versjonsnummer??
        }
    }
}
