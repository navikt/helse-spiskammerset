package no.nav.helse.spiskammerset.spiskammerset.reisverk

import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.spiskammerset.oppbevaringsboks.Oppbevaringsboks
import javax.sql.DataSource

internal fun Route.oppbevaringsbokserApi(dataSource: DataSource, oppbevaringsbokser: List<Oppbevaringsboks>) {
    oppbevaringsbokser.forEach { oppbevaringsboks ->
        get("/behandling/{behandlingId}/${oppbevaringsboks.etikett}") {
            håndterRequest {
                val behandlingId = BehandlingId.fraStreng(call.parameters["behandlingId"]) ?: return@håndterRequest call.respondFeil(httpStatusCode = BadRequest, eksponertFeilmelding = "Melding uten behandlingId blir litt rart, eller?")
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }

    get("/behandling/{behandlingId}") {
        håndterRequest {
            val etterspurteOpplysninger = (call.queryParameters.getAll("opplysning")?.toSet() ?: emptySet())
            val behandlingId = BehandlingId.fraStreng(call.parameters["behandlingId"]) ?: return@håndterRequest call.respondFeil(httpStatusCode = BadRequest, eksponertFeilmelding = "Melding uten behandlingId blir litt rart, eller?")

            call.respondFeil(httpStatusCode = NotFound, eksponertFeilmelding = "Fant ikke behandling med behandlingId: $behandlingId")
        }
    }
}
