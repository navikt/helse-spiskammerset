package no.nav.helse.spiskammerset.spiskammerset.reisverk

import com.github.navikt.tbd_libs.sql_dsl.firstOrNull
import com.github.navikt.tbd_libs.sql_dsl.localDate
import com.github.navikt.tbd_libs.sql_dsl.mapNotNull
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.singleOrNull
import com.github.navikt.tbd_libs.sql_dsl.string
import com.github.navikt.tbd_libs.sql_dsl.stringOrNull
import com.github.navikt.tbd_libs.sql_dsl.uuid
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import kotlin.use
import no.nav.helse.spiskammerset.oppbevaringsboks.Hyllenummer
import org.intellij.lang.annotations.Language

data class FunnetHylle(
    val hyllenummer: Hyllenummer,
    val behandling: Behandling.KomplettBehandling
)

interface Hyllestatus {
    val hyllenummer: Hyllenummer
    data class NyHylle(override val hyllenummer: Hyllenummer): Hyllestatus
    data class EndretHylle(override val hyllenummer: Hyllenummer): Hyllestatus
    data class UendretHylle(override val hyllenummer: Hyllenummer): Hyllestatus
}

internal fun Connection.finnRettHylle(personidentifikator: Personidentifikator, behandling: Behandling) = when (behandling) {
    is Behandling.KomplettBehandling -> finnRettHylle(personidentifikator, behandling)
    is Behandling.MinimalBehandling -> finnRettHylle(behandling)
}

private fun Connection.finnRettHylle(personidentifikator: Personidentifikator, behandling: Behandling.KomplettBehandling): Hyllestatus {
    @Language("PostgreSQL")
    val sql = """
        INSERT INTO hylle (personidentifikator, vedtaksperiode_id, behandling_id, yrkesaktivitetstype, organisasjonsnummer, fom, tom) 
        VALUES (:personidentifikator, :vedtaksperiodeId, :behandlingId, :yrkesaktivitetstype, :organisasjonsnummer, :fom, :tom) 
        ON CONFLICT ON CONSTRAINT unik_behandling_id DO NOTHING 
        RETURNING hyllenummer;
    """

    val hyllenummer = prepareStatementWithNamedParameters(sql) {
        withParameter("personidentifikator", personidentifikator.id)
        withParameter("vedtaksperiodeId", behandling.vedtaksperiodeId.id)
        withParameter("behandlingId", behandling.behandlingId.id)
        withParameter("yrkesaktivitetstype", behandling.yrkesaktivitetstype.type)
        if (behandling.organisasjonsnummer == null) withNull("organisasjonsnummer")
        else withParameter("organisasjonsnummer", behandling.organisasjonsnummer.organisasjonsnummer)
        withParameter("fom", behandling.periode.fom)
        withParameter("tom", behandling.periode.tom)
    }.firstOrNull(ResultSet::hyllenummer) ?: return finnRettHylle(Behandling.MinimalBehandling(behandlingId = behandling.behandlingId, periode = behandling.periode))

    return Hyllestatus.NyHylle(hyllenummer)
}

private fun Connection.finnRettHylle(behandling: Behandling.MinimalBehandling): Hyllestatus {
    @Language("PostgreSQL")
    val finnHylle = """SELECT hyllenummer from hylle where behandling_id = :behandlingId"""

    val hyllenummer =  prepareStatementWithNamedParameters(finnHylle) {
        withParameter("behandlingId", behandling.behandlingId.id)
    }.singleOrNull(ResultSet::hyllenummer) ?: error("Finner ingen behandling med behandlingId ${behandling.behandlingId}. Kan ikke bygge videre før den opprettes.")

    val gjeldendePeriode = behandling.periode ?: return Hyllestatus.UendretHylle(hyllenummer)

    @Language("PostgreSQL")
    val oppdatertPeriode = """
        UPDATE hylle 
        SET fom = :fom, tom = :tom
        WHERE behandling_id = :behandlingId AND (fom != :fom OR tom != :tom)
        RETURNING hyllenummer
    """

    val endretPeriode = prepareStatementWithNamedParameters(oppdatertPeriode) {
        withParameter("behandlingId", behandling.behandlingId.id)
        withParameter("fom", gjeldendePeriode.fom)
        withParameter("tom", gjeldendePeriode.tom)
    }.use(PreparedStatement::execute)

    return when (endretPeriode) {
        true -> Hyllestatus.EndretHylle(hyllenummer)
        false -> Hyllestatus.UendretHylle(hyllenummer)
    }
}

private fun ResultSet.hyllenummer() = Hyllenummer(nummer = getLong("hyllenummer"))

internal fun Connection.finnHyller(periode: Periode, vararg personidentifikatorer: Personidentifikator): List<FunnetHylle> {

    @Language("PostgreSQL")
    val sql = """
        SELECT * FROM hylle
        WHERE personidentifikator = ANY(:personidentifikatorer)
        AND periode && daterange(:fom, :tom + 1, '[)'); 
    """

    return prepareStatementWithNamedParameters(sql) {
        withParameter("personidentifikatorer", personidentifikatorer.map { it.id })
        withParameter("fom", periode.fom)
        withParameter("tom", periode.tom)
    }.mapNotNull { resultset -> FunnetHylle(
        hyllenummer = Hyllenummer(resultset.getLong("hyllenummer")),
        behandling = Behandling.KomplettBehandling(
            vedtaksperiodeId = VedtaksperiodeId(resultset.uuid("vedtaksperiode_id")),
            behandlingId = BehandlingId(resultset.uuid("behandling_id")),
            periode = Periode(resultset.localDate("fom"), resultset.localDate("tom")),
            yrkesaktivitetstype = Yrkesaktivitetstype(resultset.string("yrkesaktivitetstype")),
            organisasjonsnummer = resultset.stringOrNull("organisasjonsnummer")?.let { Organisasjonsnummer(it) }
        )
    )}
}
