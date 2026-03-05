package no.nav.helse.spiskammerset.spiskammerset.reisverk

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.transaction
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.spiskammerset.oppbevaringsboks.Oppbevaringsboks
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
