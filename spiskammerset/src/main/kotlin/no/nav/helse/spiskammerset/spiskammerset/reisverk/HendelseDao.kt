package no.nav.helse.spiskammerset.spiskammerset.reisverk

import com.github.navikt.tbd_libs.sql_dsl.firstOrNull
import com.github.navikt.tbd_libs.sql_dsl.long
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.single
import org.intellij.lang.annotations.Language
import java.sql.Connection

internal fun Connection.håndtertTidligere(hendelse: Hendelse): Boolean {
    @Language("PostgreSQL")
    val finnHendelse = """SELECT id from hendelse WHERE hendelse_id = :hendelseId"""

    return prepareStatementWithNamedParameters(finnHendelse) {
        withParameter("hendelseId", hendelse.hendelseId.id)
    }.firstOrNull { it.long("id") } != null
}

internal fun Connection.lagreHendelse(hendelse: Hendelse) {
    // TODO koble til reisverket til boksen
    @Language("PostgreSQL")
    val leggInnHendelse = """
        INSERT INTO hendelse (hendelse_id, hendelsetype, hendelse) 
        VALUES (:hendelseId, :hendelsetype, to_jsonb(:hendelse)) 
        RETURNING id;
    """

    prepareStatementWithNamedParameters(leggInnHendelse) {
        withParameter("hendelseId", hendelse.hendelseId.id)
        withParameter("hendelsetype", hendelse.hendelsetype)
        withParameter("hendelse", hendelse.json.toString())
    }.single { it.long("id") }
}
