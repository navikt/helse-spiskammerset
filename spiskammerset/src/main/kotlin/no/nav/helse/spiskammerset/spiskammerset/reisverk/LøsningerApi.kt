package no.nav.helse.spiskammerset.spiskammerset.reisverk

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.transaction
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.spiskammerset.oppbevaringsboks.Oppbevaringsboks
import java.net.URI
import javax.sql.DataSource

internal fun Route.lagreLøsningerApi(dataSource: DataSource, oppbevaringsbokser: List<Oppbevaringsboks>) {
    post("/lagre-losninger") {
        håndterRequest {
            val komplettLøsning = call.json()
            val lagredeLøsningIder = dataSource.connection {
                transaction {
                    komplettLøsning["@løsning"].properties().mapNotNull { (løsningsnavn, løsning) ->
                        val passendeOppbevaringsboks = oppbevaringsbokser.firstOrNull { it.etikett == løsningsnavn }

                        if (passendeOppbevaringsboks == null) null
                        else passendeOppbevaringsboks to løsning as ObjectNode
                    }.associate { (oppbevaringsboks, løsning) ->
                        oppbevaringsboks.etikett to oppbevaringsboks.puttI(løsning, this)
                    }
                }
            }

            call.respond(HttpStatusCode.OK, mapOf("lagredeLøsningIder" to lagredeLøsningIder))
        }
    }
}

internal fun Route.hentLøsningerApi(dataSource: DataSource, oppbevaringsbokser: List<Oppbevaringsboks>) {
    get("/opplysning/{lagringId}") {
        håndterRequest {
            val lagringId = LagringId(URI(call.parameters["lagringId"].toString()))
            val oppbevaringsboks = oppbevaringsbokser.firstOrNull { it.etikett == lagringId.etikett }
                ?: return@håndterRequest call.respond(HttpStatusCode.NotFound)
            val innhold = dataSource.connection {
                oppbevaringsboks.taUt(lagringId.id, this)
            } ?: return@håndterRequest call.respond(HttpStatusCode.NotFound)

            call.respond(HttpStatusCode.OK, innhold.tilJson())
        }
    }
}

data class LagringId(private val uri: URI) {
    init {
        require(uri.scheme.lowercase() == "urn")
        require(uri.schemeSpecificPart.split(":").size == 2)
    }

    val etikett: String = requireNotNull(uri.schemeSpecificPart.split(":").first().takeUnless { it.isBlank() })
    val id: String = requireNotNull(uri.schemeSpecificPart.split(":").last().takeUnless { it.isBlank() })
}
