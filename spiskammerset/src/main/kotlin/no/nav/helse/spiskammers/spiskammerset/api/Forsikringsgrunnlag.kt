package no.nav.helse.spiskammers.spiskammerset.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.spiskammers.spiskammerset.db.ForsikringDao
import org.intellij.lang.annotations.Language
import java.util.*

private val objectmapper = jacksonObjectMapper()
internal suspend fun ApplicationCall.requestJson() = objectmapper.readTree(receiveText())
internal suspend fun ApplicationCall.respondJson(@Language("JSON") json: String, statusCode: HttpStatusCode) = respondText(text = json, contentType = Json, status = statusCode)

internal fun Route.forsikring(dao: ForsikringDao) {
    post("/forsikring") {
        val request = call.requestJson()
        val hendelseId = UUID.fromString(request.path("@id").asText())
        val behandlingId = UUID.fromString(request.path("behandlingId").asText())
        val forsikring = request.path("forsikring").map {
            Forsikringsgrunnlag(
                hendelseId = hendelseId,
                behandlingId = behandlingId,
                dekningsgrad = it.path("dekningsgrad").asInt(),
                navOvertarAnsvarForVentetid = it.path("navOvertarAnsvarForVentetid").asBoolean()
            )
        }.single()

        dao.lagre(forsikringsgrunnlag = forsikring)
        call.respondJson("", Created)
    }

    get("/forsikring/{behandlingId}") {
        val behandlingId = call.parameters["behandlingId"]?.let { UUID.fromString(it) }
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or invalid behandlingId")
        val forsikringer = dao.hentAlle(behandlingId)
        call.respond(HttpStatusCode.OK, forsikringer)
    }
}

data class Forsikringsgrunnlag(val hendelseId: UUID, val behandlingId: UUID, val dekningsgrad: Int, val navOvertarAnsvarForVentetid: Boolean)
