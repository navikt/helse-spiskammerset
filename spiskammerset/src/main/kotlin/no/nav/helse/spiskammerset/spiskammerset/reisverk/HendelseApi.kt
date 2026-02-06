package no.nav.helse.spiskammerset.spiskammerset.reisverk

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

internal fun Route.hendelseApi(hendelsehåndterer: Hendelsehåndterer) {
    post("/hendelse") {
        håndterRequest {
            hendelsehåndterer.håndter(call.json())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
