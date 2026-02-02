package no.nav.helse.spiskammerset.spiskammerset.reisverk

import com.github.navikt.tbd_libs.sql_dsl.firstOrNull
import com.github.navikt.tbd_libs.sql_dsl.long
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.single
import java.sql.Connection
import java.sql.PreparedStatement
import no.nav.helse.spiskammerset.oppbevaringsboks.Hyllenummer
import org.intellij.lang.annotations.Language

internal fun Connection.håndterTidligere(hendelse: Hendelse): Boolean {
    @Language("PostgreSQL")
    val finnHylle = """SELECT id from hendelse WHERE hendelse_id = :hendelseId"""

    return prepareStatementWithNamedParameters(finnHylle) {
        withParameter("hendelseId", hendelse.hendelseId.id)
    }.firstOrNull { it.long("id") } != null
}

internal fun Connection.lagreHendelse(hendelse: Hendelse, endredeHyller: Set<Hyllenummer>) {
    check(endredeHyller.isNotEmpty()) {  "Ingen endrede hyller?? Hva skal det bety??" }

    @Language("PostgreSQL")
    val leggInnHendelse = """
        INSERT INTO hendelse (hendelse_id, hendelsetype, hendelse) 
        VALUES (:hendelseId, :hendelsetype, to_jsonb(:hendelse)) 
        RETURNING id;
    """

    val interntHyllenummer = prepareStatementWithNamedParameters(leggInnHendelse) {
        withParameter("hendelseId", hendelse.hendelseId.id)
        withParameter("hendelsetype", hendelse.hendelsetype)
        withParameter("hendelse", hendelse.json.toString())
    }.single { it.long("id") }


    @Language("PostgreSQL")
    val leggHendelserPåHylla = """
        INSERT INTO hendelser_paa_hylla (intern_hendelse_id, hyllenummer) 
        VALUES ${endredeHyller.joinToString { "($interntHyllenummer, ${it.nummer})" }}
    """

    prepareStatement(leggHendelserPåHylla).use(PreparedStatement::execute)
}
