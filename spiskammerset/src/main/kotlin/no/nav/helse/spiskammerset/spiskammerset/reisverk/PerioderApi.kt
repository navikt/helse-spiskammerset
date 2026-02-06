package no.nav.helse.spiskammerset.spiskammerset.reisverk

import com.github.navikt.tbd_libs.sql_dsl.connection
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.spiskammerset.spiskammerset.json

internal fun Route.perioderApi(dataSource: DataSource) {

    post("/perioder") {
        try {
            val requestJson = call.json()
            // dra ut personidentifikatorer fra call
            val personidentifikatorer = requestJson["personidentifikatorer"].map { it.asText() }.map { Personidentifikator(it) }.toTypedArray()
            // dra ut fom/tom fra call
            val fom = LocalDate.parse(requestJson["fom"].asText())
            val tom = LocalDate.parse(requestJson["tom"].asText())
            // bruk dao til å finne alle perioder som helt eller delvis er inne i fom/tom
            val behandlinger = dataSource.connection {
                finnHyller(Periode(fom, tom), *personidentifikatorer)
            }
            // map tilbake på en fornuft måte
            call.respond(HttpStatusCode.OK, behandlinger.mapTilEndepunktformat())
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError, "Nå har det gått til skogen")
        }
    }
}

data class YrkesaktivitetSpesifikasjon(val yrkesaktivitetstype: Yrkesaktivitetstype, val organisasjonsnummer: Organisasjonsnummer?)

data class SvaretViGir(val yrkesaktiviteter: List<Yrkesaktivitet>) {
    data class Yrkesaktivitet(val yrkesaktivitetstype: String, val organisasjonsnummer: String?, val vedtaksperioder: List<Vedtaksperioder>) {
        data class Vedtaksperioder(val vedtaksperiodeId: UUID, val behandlinger: List<Behandling>) {
            data class Behandling(val behandlingId: UUID, val fom: LocalDate, val tom: LocalDate, val opprettet: OffsetDateTime)
        }
    }
}

fun List<Hylle>.mapTilEndepunktformat(): SvaretViGir {
    val groupBy: Map<YrkesaktivitetSpesifikasjon, List<Hylle>> = this.groupBy { YrkesaktivitetSpesifikasjon(it.yrkesaktivitetstype, it.organisasjonsnummer) }
    return SvaretViGir(groupBy.map { (yrkesaktivitetsgreier, behandlinger) ->
        val behandlingerPerVedtaksperiode: List<SvaretViGir.Yrkesaktivitet.Vedtaksperioder> = behandlinger.groupBy { it.vedtaksperiodeId }
            .map { (vedtaksperiodeId, kompletteBehandlinger) ->
                SvaretViGir.Yrkesaktivitet.Vedtaksperioder(
                    vedtaksperiodeId = vedtaksperiodeId.id,
                    behandlinger = kompletteBehandlinger.map { komplettBehandling ->
                        SvaretViGir.Yrkesaktivitet.Vedtaksperioder.Behandling(
                            behandlingId = komplettBehandling.behandlingId.id,
                            fom = komplettBehandling.periode.fom,
                            tom = komplettBehandling.periode.tom,
                            opprettet = komplettBehandling.opprettet
                        )
                    }
                )
            }
        SvaretViGir.Yrkesaktivitet(yrkesaktivitetstype = yrkesaktivitetsgreier.yrkesaktivitetstype.type, organisasjonsnummer = yrkesaktivitetsgreier.organisasjonsnummer?.organisasjonsnummer, vedtaksperioder = behandlingerPerVedtaksperiode)
    })
}
