package no.nav.helse.spiskammerset.spiskammerset.reisverk

import com.github.navikt.tbd_libs.sql_dsl.localDate
import com.github.navikt.tbd_libs.sql_dsl.mapNotNull
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.string
import com.github.navikt.tbd_libs.sql_dsl.stringOrNull
import com.github.navikt.tbd_libs.sql_dsl.uuid
import java.sql.Connection
import kotlin.use
import no.nav.helse.spiskammerset.oppbevaringsboks.Hyllenummer
import org.intellij.lang.annotations.Language

internal fun Connection.finnRettHylle(hendelseId: HendelseId, personidentifikator: Personidentifikator, behandling: Behandling): Hyllenummer {

    @Language("PostgreSQL")
    val sql = """
        INSERT INTO hylle (personidentifikator, vedtaksperiode_id, behandling_id, yrkesaktivitetstype, organisasjonsnummer, fom, tom, hendelse_ider) 
        VALUES (:personidentifikator, :vedtaksperiodeId, :behandlingId, :yrkesaktivitetstype, :organisasjonsnummer, :fom, :tom, ARRAY[CAST(:hendelseId AS uuid)]) 
        ON CONFLICT ON CONSTRAINT unik_behandling_id DO UPDATE SET fom = EXCLUDED.fom, tom = EXCLUDED.tom, hendelse_ider = hylle.hendelse_ider || CAST(:hendelseId AS uuid) 
        WHERE hylle.vedtaksperiode_id = EXCLUDED.vedtaksperiode_id AND hylle.personidentifikator = EXCLUDED.personidentifikator
        RETURNING hyllenummer;
    """

    return prepareStatementWithNamedParameters(sql) {
        withParameter("personidentifikator", personidentifikator.id)
        withParameter("vedtaksperiodeId", behandling.vedtaksperiodeId.id)
        withParameter("behandlingId", behandling.behandlingId.id)
        withParameter("yrkesaktivitetstype", behandling.yrkesaktivitetstype.type)
        if (behandling.organisasjonsnummer == null) withNull("organisasjonsnummer")
        else withParameter("organisasjonsnummer", behandling.organisasjonsnummer.organisasjonsnummer)
        withParameter("fom", behandling.periode.fom)
        withParameter("tom", behandling.periode.tom)
        withParameter("hendelseId", hendelseId.id.toString())
    }.executeQuery().use { resultset ->
        Hyllenummer(
            nummer = resultset.use { rs ->
                check(rs.next()) { "Hva skjedde nå?" }
                rs.getLong(1)
            }
        )
    }
}


data class Hylle(
    val hyllenummer: Hyllenummer,
    val behandling: Behandling
)

internal fun Connection.finnHyller(personidentifikator: Personidentifikator, periode: Periode): List<Hylle> {

    @Language("PostgreSQL")
    val sql = """
        SELECT * FROM hylle
        WHERE personidentifikator = :personidentifikator
        AND periode && daterange(:fom, :tom + 1, '[)'); 
    """

    return prepareStatementWithNamedParameters(sql) {
        withParameter("personidentifikator", personidentifikator.id)
        withParameter("fom", periode.fom)
        withParameter("tom", periode.tom)
    }.mapNotNull { resultset -> Hylle(
        hyllenummer = Hyllenummer(resultset.getLong("hyllenummer")),
        behandling = Behandling(
            vedtaksperiodeId = VedtaksperiodeId(resultset.uuid("vedtaksperiode_id")),
            behandlingId = BehandlingId(resultset.uuid("behandling_id")),
            periode = Periode(resultset.localDate("fom"), resultset.localDate("tom")),
            yrkesaktivitetstype = Yrkesaktivitetstype(resultset.string("yrkesaktivitetstype")),
            organisasjonsnummer = resultset.stringOrNull("organisasjonsnummer")?.let { Organisasjonsnummer(it) }
        )
    )}
}
