package no.nav.helse.spiskammerset.forsikring

import com.fasterxml.jackson.databind.JsonNode
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

    override fun puttI(json: JsonNode, connection: Connection): UUID {
        val dao = ForsikringDao(connection)
        // TODO 1: Vi må lagre forsikringer på en annen måte for det kan være fler enn én
        // TODO 2: At det ikke er noen forsikring må også lagres ned og knyttes mot en ID
        val denFørsteForsikringen = json.first()

        val forsikring = Forsikring(
            forsikringstype = denFørsteForsikringen["forsikringstype"].asText(),
            premiegrunnlag = denFørsteForsikringen["premiegrunnlag"].asInt(),
            startdato = LocalDate.parse(denFørsteForsikringen["startdato"].asText()),
            sluttdato = denFørsteForsikringen.path("sluttdato").takeUnless { it.isMissingNode || it.isNull }?.let { LocalDate.parse(it.asText()) },
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
