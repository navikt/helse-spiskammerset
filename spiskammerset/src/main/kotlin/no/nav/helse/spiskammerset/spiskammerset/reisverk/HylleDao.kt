package no.nav.helse.spiskammerset.spiskammerset.reisverk

import com.github.navikt.tbd_libs.sql_dsl.firstOrNull
import com.github.navikt.tbd_libs.sql_dsl.localDate
import com.github.navikt.tbd_libs.sql_dsl.mapNotNull
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.string
import com.github.navikt.tbd_libs.sql_dsl.stringOrNull
import com.github.navikt.tbd_libs.sql_dsl.uuid
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.time.OffsetDateTime
import kotlin.use
import no.nav.helse.spiskammerset.oppbevaringsboks.Hyllenummer
import org.intellij.lang.annotations.Language

data class Hylle(
    val behandlingId: BehandlingId,
    val vedtaksperiodeId: VedtaksperiodeId,
    val periode: Periode,
    val yrkesaktivitetstype: Yrkesaktivitetstype,
    val organisasjonsnummer: Organisasjonsnummer?,
    val opprettet: OffsetDateTime = OffsetDateTime.MIN
)

sealed interface Hyllestatus {
    val hyllenummer: Hyllenummer
    data class NyHylle(override val hyllenummer: Hyllenummer): Hyllestatus // Hyllen ble opprettet nå
    data class EndretHylle(override val hyllenummer: Hyllenummer): Hyllestatus // Informasjon på hyllen ble endret
    data class UendretHylle(override val hyllenummer: Hyllenummer): Hyllestatus // Intet nytt fra Vestfronten
}

internal fun Connection.finnEllerOpprettHylle(behandling: Behandling) = when (behandling) {
    is Behandling.KomplettBehandling -> finnEllerOpprettHylle(behandling)
    is Behandling.MinimalBehandling -> finnEllerOpprettHylle(behandling)
}

private fun Connection.finnEllerOpprettHylle(behandling: Behandling.KomplettBehandling): Hyllestatus {
    @Language("PostgreSQL")
    val sql = """
        INSERT INTO hylle (vedtaksperiode_id, behandling_id, yrkesaktivitetstype, organisasjonsnummer, fom, tom) 
        VALUES (:vedtaksperiodeId, :behandlingId, :yrkesaktivitetstype, :organisasjonsnummer, :fom, :tom) 
        ON CONFLICT ON CONSTRAINT unik_behandling_id DO NOTHING 
        RETURNING hyllenummer;
    """

    val hyllenummer = prepareStatementWithNamedParameters(sql) {
        withParameter("vedtaksperiodeId", behandling.vedtaksperiodeId.id)
        withParameter("behandlingId", behandling.behandlingId.id)
        withParameter("yrkesaktivitetstype", behandling.yrkesaktivitetstype.type)
        if (behandling.organisasjonsnummer == null) withNull("organisasjonsnummer")
        else withParameter("organisasjonsnummer", behandling.organisasjonsnummer.organisasjonsnummer)
        withParameter("fom", behandling.periode.fom)
        withParameter("tom", behandling.periode.tom)
    }.firstOrNull(ResultSet::hyllenummer) ?: return finnEllerOpprettHylle(Behandling.MinimalBehandling(
        behandlingId = behandling.behandlingId,
        periode = behandling.periode,
        personidentifikator = behandling.personidentifikator
    ))

    nyHylleeier(hyllenummer, behandling)
    return Hyllestatus.NyHylle(hyllenummer)
}

private fun Connection.finnEllerOpprettHylle(behandling: Behandling.MinimalBehandling): Hyllestatus {
    @Language("PostgreSQL")
    val finnHylle = """SELECT hyllenummer from hylle where behandling_id = :behandlingId"""

    val hyllenummer =  prepareStatementWithNamedParameters(finnHylle) {
        withParameter("behandlingId", behandling.behandlingId.id)
    }.firstOrNull(ResultSet::hyllenummer) ?: error("Finner ingen behandling med behandlingId ${behandling.behandlingId}. Kan ikke bygge videre før den opprettes.")

    val gjeldendePeriode = behandling.periode ?: return when (nyHylleeier(hyllenummer, behandling)) {
        true -> Hyllestatus.EndretHylle(hyllenummer)
        false -> Hyllestatus.UendretHylle(hyllenummer)
    }

    @Language("PostgreSQL")
    val oppdatertPeriodeSql = """
        UPDATE hylle 
        SET fom = :fom, tom = :tom
        WHERE behandling_id = :behandlingId AND (fom != :fom OR tom != :tom)
    """

    val endretPeriode = prepareStatementWithNamedParameters(oppdatertPeriodeSql) {
        withParameter("behandlingId", behandling.behandlingId.id)
        withParameter("fom", gjeldendePeriode.fom)
        withParameter("tom", gjeldendePeriode.tom)
    }.use(PreparedStatement::executeUpdate) == 1


    return when (nyHylleeier(hyllenummer, behandling) || endretPeriode) {
        true -> Hyllestatus.EndretHylle(hyllenummer)
        false -> Hyllestatus.UendretHylle(hyllenummer)
    }
}

private fun ResultSet.hyllenummer() = Hyllenummer(nummer = getLong("hyllenummer"))

private fun Connection.nyHylleeier(hyllenummer: Hyllenummer, behandling: Behandling): Boolean {
    @Language("PostgreSQL")
    val nyPersonidentifikatorSql = """
        INSERT INTO hylleeier (hyllenummer, personidentifikator)
        VALUES (:hyllenummer, :personidentifikator)
        ON CONFLICT DO NOTHING 
    """

    @Language("PostgreSQL")
    return prepareStatementWithNamedParameters(nyPersonidentifikatorSql) {
        withParameter("hyllenummer", hyllenummer.nummer)
        withParameter("personidentifikator", behandling.personidentifikator.id)
    }.use(PreparedStatement::executeUpdate) == 1
}

internal fun Connection.finnHyller(periode: Periode, vararg personidentifikatorer: Personidentifikator): List<Hylle> {
    @Language("PostgreSQL")
    val sql = """
        SELECT h.*
        FROM hylle h
        WHERE EXISTS (
            SELECT 1
            FROM hylleeier he
            WHERE he.hyllenummer = h.hyllenummer
            AND he.personidentifikator = ANY(:personidentifikatorer)
        )
        AND EXISTS (
            SELECT 1
            FROM hylle h2
            WHERE h2.vedtaksperiode_id = h.vedtaksperiode_id
            AND h2.periode && daterange(:fom, :tom + 1, '[)')
        );
    """

    return prepareStatementWithNamedParameters(sql) {
        withParameter("personidentifikatorer", personidentifikatorer.map { it.id })
        withParameter("fom", periode.fom)
        withParameter("tom", periode.tom)
    }.mapNotNull { resultset -> Hylle(
        vedtaksperiodeId = VedtaksperiodeId(resultset.uuid("vedtaksperiode_id")),
        behandlingId = BehandlingId(resultset.uuid("behandling_id")),
        periode = Periode(resultset.localDate("fom"), resultset.localDate("tom")),
        yrkesaktivitetstype = Yrkesaktivitetstype(resultset.string("yrkesaktivitetstype")),
        organisasjonsnummer = resultset.stringOrNull("organisasjonsnummer")?.let { Organisasjonsnummer(it) }
    )}
}
