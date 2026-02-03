package no.nav.helse.spiskammerset.spiskammerset.reisverk

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.sql_dsl.connection
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.spiskammerset.spiskammerset.reisverk.SvaretViGir.Yrkesaktivitet.Vedtaksperioder

private val objectmapper = jacksonObjectMapper()
private suspend fun ApplicationCall.json() = objectmapper.readTree(receiveText()) as ObjectNode

internal fun Route.hendelse(hendelsehåndterer: Hendelsehåndterer) {
    post("/hendelse") {
        try {
            hendelsehåndterer.håndter(call.json())
            call.respond(HttpStatusCode.NoContent, "Må man ha message på status. rart..")
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError, "Nå har det gått til skogen")
        }
    }
}

internal fun Route.allePerioder(dataSource: DataSource) {
    /*
    inn : """
     "personIdenter": ["a", "b", "+10110023"],
     "fom": en dato,
     "tom": en dato
    """

    ut: """
    {
      "yrkesaktiviteter": [
        {
            "yrkesaktivitetstype": "SELVSTENDIG" eller "ARBEIDSTAKER" osv,
            "orgnr": "ssdf" (men bare for arbeidstakere),
            "vedtaksperioder": [
            {
                "vedtaksperiodeId": "uuid",
                "behandlinger": [ {
                    "behandlingId": "fbsdf",
                    "fom": localDate,
                    "tom": localDate,
                    "opprettet": timestamp
                },
                 ..
                ]
            },
            ..
            ]
        },
        ..
      ]
    }
    """
     */
    post("/") {
        try {
            val requestJson = call.json()
            // dra ut personidentifikatorer fra call
            val personIdenter = requestJson["personIdenter"].map { it.asText() }.map { Personidentifikator(it) }.toTypedArray()
            // dra ut fom/tom fra call
            val fom = LocalDate.parse(requestJson["fom"].asText())
            val tom = LocalDate.parse(requestJson["tom"].asText())
            // bruk dao til å finne alle perioder som helt eller delvis er inne i fom/tom
            val behandlinger = dataSource.connection {
                finnHyller(Periode(fom, tom), *personIdenter)
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
    data class Yrkesaktivitet(val yrkesaktivitetstype: String, val orgnr: String?, val vedtaksperioder: List<Vedtaksperioder>) {
        data class Vedtaksperioder(val vedtaksperiodeId: UUID, val behandlinger: List<Behandling>) {
            data class Behandling(val behandlingId: UUID, val fom: LocalDate, val tom: LocalDate, val opprettet: OffsetDateTime)
        }
    }
}

fun List<Hylle>.mapTilEndepunktformat(): SvaretViGir {
    val groupBy: Map<YrkesaktivitetSpesifikasjon, List<Hylle>> = this.groupBy { YrkesaktivitetSpesifikasjon(it.yrkesaktivitetstype, it.organisasjonsnummer) }
    return SvaretViGir(groupBy.map { (yrkesaktivitetsgreier, behandlinger) ->
        val behandlingerPerVedtaksperiode: List<Vedtaksperioder> = behandlinger.groupBy { it.vedtaksperiodeId }
            .map { (vedtaksperiodeId, kompletteBehandlinger) ->
                Vedtaksperioder(
                    vedtaksperiodeId = vedtaksperiodeId.id,
                    behandlinger = kompletteBehandlinger.map { komplettBehandling ->
                        Vedtaksperioder.Behandling(
                            behandlingId = komplettBehandling.behandlingId.id,
                            fom = komplettBehandling.periode.fom,
                            tom = komplettBehandling.periode.tom,
                            opprettet = komplettBehandling.opprettet
                        )
                    }
                )
            }
        SvaretViGir.Yrkesaktivitet(yrkesaktivitetstype = yrkesaktivitetsgreier.yrkesaktivitetstype.type, orgnr = yrkesaktivitetsgreier.organisasjonsnummer?.organisasjonsnummer, vedtaksperioder = behandlingerPerVedtaksperiode)
    })
}
