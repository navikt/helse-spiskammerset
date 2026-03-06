package no.nav.helse.spiskammerset.forsikring

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.spiskammerset.oppbevaringsboks.Innhold
import no.nav.helse.spiskammerset.oppbevaringsboks.Oppbevaringsboks
import no.nav.helse.spiskammerset.oppbevaringsboks.Versjon
import java.sql.Connection
import java.time.LocalDate
import java.util.*

data object Forsikringsboks: Oppbevaringsboks {
    override val etikett = "forsikring"
    override val behovsnavn = setOf("SelvstendigForsikring")
    private val gjeldendeVersjon = Versjon(1)

    override fun puttI(json: ObjectNode, connection: Connection): UUID {
        val dao = ForsikringDao(connection)

        val forsikring = Forsikring(
            forsikringstype = json["forsikringstype"].asText(),
            premiegrunnlag = json["premiegrunnlag"].asInt(),
            startdato = LocalDate.parse(json["startdato"].asText()),
            sluttdato = json.path("sluttdato").takeUnless { it.isMissingNode || it.isNull }?.let { LocalDate.parse(it.asText()) },
            versjon = gjeldendeVersjon
        )

        return dao.lagre(forsikring)
    }

    override fun taUt(id: UUID, connection: Connection): Innhold? {
        val dao = ForsikringDao(connection)
        return when (val forsikring = dao.hent(id)) {
            null -> null
            else -> Innhold(forsikring.versjon, forsikring.innhold)
        }
    }
}
