package no.nav.helse.spiskammerset.forsikring

import com.github.navikt.tbd_libs.sql_dsl.boolean
import com.github.navikt.tbd_libs.sql_dsl.int
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.singleOrNull
import no.nav.helse.spiskammerset.oppbevaringsboks.Hyllenummer
import no.nav.helse.spiskammerset.oppbevaringsboks.Versjon
import org.intellij.lang.annotations.Language
import java.sql.Connection

internal class ForsikringDao(private val connection: Connection) {

    internal fun lagre(forsikring: Forsikring, hyllenummer: Hyllenummer): Int {
        @Language("PostgreSQL")
        val query = """ 
            INSERT INTO forsikring (dekningsgrad, nav_overtar_ansvar_for_ventetid, premiegrunnlag, hyllenummer, versjon) 
            VALUES (:dekningsgrad, :nav_overtar_ansvar_for_ventetid, :premiegrunnlag, :hyllenummer, :versjon) 
            ON CONFLICT DO NOTHING 
            """

        return connection.prepareStatementWithNamedParameters(query) {
            withParameter("dekningsgrad", forsikring.dekningsgrad)
            withParameter("nav_overtar_ansvar_for_ventetid", forsikring.navOvertarAnsvarForVentetid)
            withParameter("premiegrunnlag", forsikring.premiegrunnlag)
            withParameter("hyllenummer", hyllenummer.nummer)
            withParameter("versjon", forsikring.versjon.nummer)
        }.use { it.executeUpdate() }
    }

    internal fun hent(hyllenummer: Hyllenummer): Forsikring? {
        @Language("PostgreSQL")
        val query =
            """ SELECT dekningsgrad, nav_overtar_ansvar_for_ventetid, premiegrunnlag, versjon FROM forsikring WHERE hyllenummer=:hyllenummer """

        return connection.prepareStatementWithNamedParameters(query) {
            withParameter("hyllenummer", hyllenummer.nummer)
        }.singleOrNull {
            Forsikring(
                dekningsgrad = it.int("dekningsgrad"),
                navOvertarAnsvarForVentetid = it.boolean("nav_overtar_ansvar_for_ventetid"),
                premiegrunnlag = it.int("premiegrunnlag"),
                versjon = Versjon(it.int("versjon"))
            )
        }
    }
}
