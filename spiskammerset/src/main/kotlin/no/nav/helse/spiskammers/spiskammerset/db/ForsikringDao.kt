package no.nav.helse.spiskammers.spiskammerset.db

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spiskammers.spiskammerset.api.Forsikringsgrunnlag
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

internal class ForsikringDao(private val dataSource: DataSource) {

    internal fun lagre(forsikringsgrunnlag: Forsikringsgrunnlag) {
        @Language("PostgreSQL")
        val query = """ INSERT INTO forsikring (hendelse_id, behandling_id, dekningsgrad, nav_overtar_ansvar_for_ventetid) VALUES (:hendelseId, :behandlingId, :dekningsgrad, :navOvertarAnsvarForVentetid) ON CONFLICT DO NOTHING """
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "hendelseId" to forsikringsgrunnlag.hendelseId,
                        "behandlingId" to forsikringsgrunnlag.behandlingId,
                        "dekningsgrad" to forsikringsgrunnlag.dekningsgrad,
                        "navOvertarAnsvarForVentetid" to forsikringsgrunnlag.navOvertarAnsvarForVentetid,
                    )
                ).asExecute
            )
        }
    }

    internal fun hent(behandlingId: UUID): Forsikringsgrunnlag? {
        @Language("PostgreSQL")
        val query =
            """ SELECT hendelse_id, behandling_id, dekningsgrad, nav_overtar_ansvar_for_ventetid FROM forsikring WHERE behandling_id=? """
        sessionOf(dataSource).use { session ->
            return session.run(
                queryOf(
                    query,
                    behandlingId
                ).map {
                    Forsikringsgrunnlag(
                        it.uuid("hendelse_id"),
                        it.uuid("behandling_id"),
                        it.int("dekningsgrad"),
                        it.boolean("nav_overtar_ansvar_for_ventetid")
                    )
                }.asSingle
            )
        }
    }
}
