package no.nav.helse.spiskammerset.spiskammerset.reisverk

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.naisful.FeilResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.callid.callId
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.queryString
import io.ktor.server.request.receiveText
import io.ktor.server.request.uri
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import java.net.URI
import no.nav.helse.spiskammerset.spiskammerset.objectmapper
import no.nav.helse.spiskammerset.spiskammerset.sikkerlogg

internal suspend fun ApplicationCall.json() = objectmapper.readTree(receiveText()) as ObjectNode

internal suspend fun ApplicationCall.respondFeil(
    error: Throwable? = null,
    httpStatusCode: HttpStatusCode = HttpStatusCode.InternalServerError,
    eksponertFeilmelding: String? = null
) {
    error?.let { sikkerlogg.error("Feil ved håndtering av ${request.httpMethod.value}@${request.path()}${request.queryString()}", it) }

    response.header("Content-Type", ContentType.Application.ProblemJson.toString())

    respond(httpStatusCode, FeilResponse(
        status = httpStatusCode,
        type = URI("urn:error:${httpStatusCode.description.lowercase().replace(" ","_")}"),
        detail = eksponertFeilmelding ?: error?.message,
        instance = URI(request.uri),
        callId = callId,
        stacktrace = eksponertFeilmelding ?: error?.stackTraceToString()
    ))
}

internal suspend fun RoutingContext.håndterRequest(block: suspend RoutingContext.() -> Unit) = try { block() } catch (error: Throwable) { call.respondFeil(error)}
