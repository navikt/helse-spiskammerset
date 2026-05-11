package no.nav.helse.spiskammerset

/*
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
*/
