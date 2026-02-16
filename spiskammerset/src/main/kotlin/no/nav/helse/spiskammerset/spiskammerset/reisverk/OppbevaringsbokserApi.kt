package no.nav.helse.spiskammerset.spiskammerset.reisverk

import com.github.navikt.tbd_libs.sql_dsl.connection
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import javax.sql.DataSource
import kotlin.collections.forEach
import no.nav.helse.spiskammerset.oppbevaringsboks.Innhold
import no.nav.helse.spiskammerset.oppbevaringsboks.Innhold.Companion.tilJson
import no.nav.helse.spiskammerset.oppbevaringsboks.Oppbevaringsboks
import java.time.LocalDate

internal fun Route.oppbevaringsbokserApi(dataSource: DataSource, oppbevaringsbokser: List<Oppbevaringsboks>) {
    oppbevaringsbokser.forEach { oppbevaringsboks ->
        get("/behandling/{behandlingId}/${oppbevaringsboks.etikett}") {
            håndterRequest {
                val behandlingId = BehandlingId.fraStreng(call.parameters["behandlingId"]) ?: return@håndterRequest call.respondFeil(httpStatusCode = BadRequest, eksponertFeilmelding = "Melding uten behandlingId blir litt rart, eller?")
                val (hyllenummer, innhold) = dataSource.connection {
                    val hyllenummer = finnHyllenummer(behandlingId) ?: return@connection null to null
                    hyllenummer to oppbevaringsboks.taNedFra(hyllenummer, this)
                }

                if (hyllenummer == null) {
                    return@håndterRequest call.respondFeil(httpStatusCode = NotFound, eksponertFeilmelding = "Fant ikke ${oppbevaringsboks.etikett} for behandlingId: $behandlingId")
                }

                when (innhold) {
                    null -> call.respond(HttpStatusCode.NoContent)
                    else -> call.respond(HttpStatusCode.OK, innhold.tilJson())
                }
            }
        }
    }

    get("/behandling/{behandlingId}") {
        håndterRequest {
            val etterspurteOpplysninger = (call.queryParameters.getAll("opplysning")?.toSet() ?: emptySet())
            val behandlingId = BehandlingId.fraStreng(call.parameters["behandlingId"]) ?: return@håndterRequest call.respondFeil(httpStatusCode = BadRequest, eksponertFeilmelding = "Melding uten behandlingId blir litt rart, eller?")

            val etterspurteOppbevaringsbokser = etterspurteOpplysninger.mapNotNull { etikett -> oppbevaringsbokser.firstOrNull { it.etikett == etikett } }

            val (hyllenummer, altInnhold) = dataSource.connection {
                val hyllenummer = finnHyllenummer(behandlingId) ?: return@connection null to emptyMap<String, Innhold?>()
                hyllenummer to etterspurteOppbevaringsbokser.associate { etterspurtOppbevaringsboks ->
                    etterspurtOppbevaringsboks.etikett to etterspurtOppbevaringsboks.taNedFra(hyllenummer, this)
                }
            }

            if (hyllenummer == null) {
                return@håndterRequest call.respondFeil(httpStatusCode = NotFound, eksponertFeilmelding = "Fant ikke behandling med behandlingId: $behandlingId")
            }

            call.respond(HttpStatusCode.OK, altInnhold.tilJson())
        }
    }

    post("/hentAlt") {
        håndterRequest {
            val requestJson = call.json()
            // dra ut personidentifikatorer fra call
            val personidentifikator = Personidentifikator(requestJson["personidentifikator"].asText())
            val etterspurteOpplysninger =
                requestJson["etterspurteOpplysninger"].map { etterspurteOpplysning -> etterspurteOpplysning.asText() }
                    .toSet()

            // bruk dao til å finne alle perioder som helt eller delvis er inne i fom/tom
            val hyller = dataSource.connection {
                finnHyller(Periode(LocalDate.of(2019, 1, 1), LocalDate.of(9999, 1, 1)), personidentifikator)
            }

            val etterspurteOppbevaringsbokser =
                etterspurteOpplysninger.mapNotNull { etikett -> oppbevaringsbokser.firstOrNull { it.etikett == etikett } }

            // Hent innhold for hver behandling og oppbevaringsboks
            val behandlingerMedInnhold = dataSource.connection {
                hyller.map { hylle ->
                    val hyllenummer = finnHyllenummer(hylle.behandlingId)
                    val innholdPerBoks = if (hyllenummer != null) {
                        etterspurteOppbevaringsbokser.associate { boks ->
                            boks.etikett to boks.taNedFra(hyllenummer, this)
                        }
                    } else {
                        emptyMap()
                    }
                    hylle to innholdPerBoks
                }
            }

            // map tilbake på en fornuftig måte
            val response = behandlingerMedInnhold.mapTilVedtaksperiodeformat()
            call.respond(HttpStatusCode.OK, response)
        }
    }

}

data class VedtaksperiodeResponse(val vedtaksperioder: List<Vedtaksperiode>) {
    data class Vedtaksperiode(
        val vedtaksperiodeId: java.util.UUID,
        val periode: PeriodeDto,
        val behandlinger: List<BehandlingMedInnhold>
    ) {
        data class PeriodeDto(val fom: LocalDate, val tom: LocalDate)
        data class BehandlingMedInnhold(
            val behandlingId: java.util.UUID,
            val oppbevaringsbokser: Map<String, Map<String, Any?>?>
        )
    }
}

typealias BehandlingMedOppbevaringsbokser = Pair<Hylle, Map<String, Innhold?>>

fun List<BehandlingMedOppbevaringsbokser>.mapTilVedtaksperiodeformat(): VedtaksperiodeResponse {
    val vedtaksperioder = this.groupBy { it.first.vedtaksperiodeId }
        .map { (vedtaksperiodeId, hyllerMedInnhold) ->
            // Finn perioden for denne vedtaksperioden
            val periode = hyllerMedInnhold.first().first.periode

            val behandlinger = hyllerMedInnhold.map { (hylle, innholdMap) ->
                // Konverter innhold til Map<String, Any?>
                val oppbevaringsbokser = innholdMap.mapValues { (_, innhold) ->
                    innhold?.innhold
                }

                VedtaksperiodeResponse.Vedtaksperiode.BehandlingMedInnhold(
                    behandlingId = hylle.behandlingId.id,
                    oppbevaringsbokser = oppbevaringsbokser
                )
            }

            VedtaksperiodeResponse.Vedtaksperiode(
                vedtaksperiodeId = vedtaksperiodeId.id,
                periode = VedtaksperiodeResponse.Vedtaksperiode.PeriodeDto(fom = periode.fom, tom = periode.tom),
                behandlinger = behandlinger
            )
        }

    return VedtaksperiodeResponse(vedtaksperioder)
}
