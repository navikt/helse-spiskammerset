package no.nav.helse.spiskammerset.spiskammerset.reisverk

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

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
