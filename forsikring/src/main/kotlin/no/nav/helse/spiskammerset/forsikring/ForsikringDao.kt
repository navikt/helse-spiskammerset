package no.nav.helse.spiskammerset.forsikring

import com.github.navikt.tbd_libs.sql_dsl.*
import no.nav.helse.spiskammerset.oppbevaringsboks.Versjon
import org.intellij.lang.annotations.Language
import java.sql.Connection
import java.util.*

internal class ForsikringDao(private val connection: Connection) {

    internal fun lagre(forsikring: Forsikring): UUID {
        @Language("PostgreSQL")
        val query = """ 
            INSERT INTO forsikring (forsikringstype, premiegrunnlag, startdato, sluttdato, versjon) 
            VALUES (:forsikringstype, :premiegrunnlag, :startdato, :sluttdato, :versjon)
            RETURNING id
            """

        return connection.prepareStatementWithNamedParameters(query) {
            withParameter("forsikringstype", forsikring.forsikringstype)
            withParameter("premiegrunnlag", forsikring.premiegrunnlag)
            withParameter("startdato", forsikring.startdato)
            if (forsikring.sluttdato != null) {
                withParameter("sluttdato", forsikring.sluttdato)
            } else {
                withNull("sluttdato")
            }
            withParameter("versjon", forsikring.versjon.nummer)
        }.single { it.uuid("id") }
    }

    internal fun hent(id: UUID): Forsikring? {
        @Language("PostgreSQL")
        val query =
            """ SELECT forsikringstype, premiegrunnlag, startdato, sluttdato, versjon FROM forsikring WHERE id =:id """

        return connection.prepareStatementWithNamedParameters(query) {
            withParameter("id", id)
        }.firstOrNull {
            Forsikring(
                forsikringstype = it.string("forsikringstype"),
                premiegrunnlag = it.int("premiegrunnlag"),
                startdato = it.localDate("startdato"),
                sluttdato = it.localDateOrNull("sluttdato"),
                versjon = Versjon(it.int("versjon"))
            )
        }
    }
}
