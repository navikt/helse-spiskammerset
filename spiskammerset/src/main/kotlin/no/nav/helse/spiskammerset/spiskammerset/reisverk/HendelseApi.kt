package no.nav.helse.spiskammerset.spiskammerset.reisverk

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.spiskammerset.spiskammerset.json

internal fun Route.hendelseApi(hendelsehåndterer: Hendelsehåndterer) {
    post("/hendelse") {
        try {
            hendelsehåndterer.håndter(call.json())
            call.respond(HttpStatusCode.NoContent)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError, "Nå har det gått til skogen")
        }
    }
}
