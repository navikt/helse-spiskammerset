package no.nav.helse.spiskammerset

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import javax.sql.DataSource

internal fun Route.hentLøsningerApi(dataSource: DataSource) {
    get("/opplysning/{lagringId}") {
        /*
    håndterRequest {
        val lagringId = LagringId(URI(call.parameters["lagringId"].toString()))
        val oppbevaringsboks = oppbevaringsbokser.firstOrNull { it.etikett == lagringId.etikett }
            ?: return@håndterRequest call.respond(HttpStatusCode.NotFound)
        val innhold = dataSource.connection {
            oppbevaringsboks.taUt(lagringId.id, this)
        } ?: return@håndterRequest call.respond(HttpStatusCode.NotFound)


        }
         */
        call.respond(HttpStatusCode.OK, "{}")
    }
}
