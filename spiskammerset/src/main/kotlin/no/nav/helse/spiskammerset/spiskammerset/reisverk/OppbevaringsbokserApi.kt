package no.nav.helse.spiskammerset.spiskammerset.reisverk

import com.github.navikt.tbd_libs.sql_dsl.connection
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import javax.sql.DataSource
import kotlin.collections.forEach
import no.nav.helse.spiskammerset.oppbevaringsboks.Innhold
import no.nav.helse.spiskammerset.oppbevaringsboks.Innhold.Companion.tilJson
import no.nav.helse.spiskammerset.oppbevaringsboks.Oppbevaringsboks

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
}
