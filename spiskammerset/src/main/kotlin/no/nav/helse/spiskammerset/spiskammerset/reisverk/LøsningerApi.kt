package no.nav.helse.spiskammerset.spiskammerset.reisverk

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.transaction
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.spiskammerset.oppbevaringsboks.Oppbevaringsboks
import java.net.URI
import java.util.*
import javax.sql.DataSource

internal fun Route.lagreLøsningerApi(dataSource: DataSource, oppbevaringsbokser: List<Oppbevaringsboks>) {
    post("/lagre-losninger") {
        håndterRequest {
            val komplettLøsning = call.json()

            val skalLagres = komplettLøsning["@løsning"]
                .properties()
                .mapNotNull { (behovsnavn, løsning) ->
                    val passendeOppbevaringsboks = oppbevaringsbokser.firstOrNull { behovsnavn in it.behovsnavn }
                    if (passendeOppbevaringsboks == null) return@mapNotNull null
                    val ignorer = komplettLøsning.path(behovsnavn).path("ignorer").asBoolean(false)
                    if (ignorer) return@mapNotNull null
                    SkalLagres(behovsnavn, passendeOppbevaringsboks, løsning as ObjectNode)
                }

            val etiketter = skalLagres.map { it.oppbevaringsboks.etikett }
            check(etiketter.size == etiketter.toSet().size) { "Meldingen inneholder flere løsninger som hører til samme oppvbevaringsboks! Dette skal ikke skje" }

            val lagredeLøsningIder = dataSource.connection {
                transaction {
                    skalLagres.associate { (behovsnavn, oppbevaringsboks, løsning) ->
                        behovsnavn to LagringId(oppbevaringsboks.etikett, oppbevaringsboks.puttI(løsning, this)).toString()
                    }
                }
            }

            call.respond(HttpStatusCode.OK, mapOf("lagringIder" to lagredeLøsningIder))
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

private data class SkalLagres(val behovsnavn: String, val oppbevaringsboks: Oppbevaringsboks, val løsning: ObjectNode)

internal data class LagringId(private val uri: URI) {
    internal constructor(etikett: String, id: UUID): this(URI("urn:grunnlagsdata:$etikett:$id"))
    private val biter = uri.schemeSpecificPart.split(":")
    init {
        require(uri.scheme.lowercase() == "urn")
        require(biter.size == 3)
        require(biter.first() == "grunnlagsdata")
    }

    val etikett = requireNotNull(biter[1].takeUnless { it.isBlank() })
    val id: UUID = UUID.fromString(requireNotNull(biter[2].takeUnless { it.isBlank() }))
    override fun toString() = uri.toString()
}
