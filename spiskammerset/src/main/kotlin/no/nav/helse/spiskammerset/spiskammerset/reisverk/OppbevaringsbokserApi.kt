package no.nav.helse.spiskammerset.spiskammerset.reisverk

import com.github.navikt.tbd_libs.sql_dsl.connection
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import javax.sql.DataSource
import kotlin.collections.forEach
import no.nav.helse.spiskammerset.oppbevaringsboks.Innhold
import no.nav.helse.spiskammerset.oppbevaringsboks.Innhold.Companion.tilJson
import no.nav.helse.spiskammerset.oppbevaringsboks.Oppbevaringsboks
import no.nav.helse.spiskammerset.spiskammerset.sikkerlogg

internal fun Route.oppbevaringsbokserApi(dataSource: DataSource, oppbevaringsbokser: List<Oppbevaringsboks>) {
    oppbevaringsbokser.forEach { oppbevaringsboks ->
        get("/behandling/{behandlingId}/${oppbevaringsboks.etikett}") {
            try {
                val behandlingId = BehandlingId.fraStreng(call.parameters["behandlingId"]) ?: return@get call.respond(HttpStatusCode.BadRequest, "Melding uten behandlingId blir litt rart, eller?")
                val (hyllenummer, innhold) = dataSource.connection {
                    val hyllenummer = finnHyllenummer(behandlingId) ?: return@connection null to null
                    hyllenummer to oppbevaringsboks.taNedFra(hyllenummer, this)
                }

                if (hyllenummer == null) {
                    return@get call.respond(HttpStatusCode.NotFound, """{ "feilmelding": "Fant ikke ${oppbevaringsboks.etikett} for behandlingId: $behandlingId" }""")
                }

                when (innhold) {
                    null -> call.respond(HttpStatusCode.NoContent)
                    else -> call.respond(HttpStatusCode.OK, innhold.tilJson())
                }
            } catch (error: Exception) {
                sikkerlogg.error("Feil ved henting av ${oppbevaringsboks.etikett}", error)
                call.respond(HttpStatusCode.InternalServerError, "Nå har det gått til skogen")
            }
        }
    }

    get("/behandling/{behandlingId}") {
        val etterspurteOpplysninger = (call.queryParameters.getAll("opplysning")?.toSet() ?: emptySet())
        val behandlingId = BehandlingId.fraStreng(call.parameters["behandlingId"]) ?: return@get call.respond(HttpStatusCode.BadRequest, "Melding uten behandlingId blir litt rart, eller?")

        try {
            val etterspurteOppbevaringsbokser = etterspurteOpplysninger.mapNotNull { etikett -> oppbevaringsbokser.firstOrNull { it.etikett == etikett } }

            val (hyllenummer, altInnhold) = dataSource.connection {
                val hyllenummer = finnHyllenummer(behandlingId) ?: return@connection null to emptyMap<String, Innhold?>()
                hyllenummer to etterspurteOppbevaringsbokser.associate { etterspurtOppbevaringsboks ->
                    etterspurtOppbevaringsboks.etikett to etterspurtOppbevaringsboks.taNedFra(hyllenummer, this)
                }
            }

            if (hyllenummer == null) {
                return@get call.respond(HttpStatusCode.NotFound, """{ "feilmelding": "Fant ikke behandling med behandlingId: $behandlingId" }""")
            }

            call.respond(HttpStatusCode.OK, altInnhold.tilJson())
        } catch (error: Exception) {
            sikkerlogg.error("Feil ved henting av $etterspurteOpplysninger for behandlingId ${behandlingId.id}", error)
            call.respond(HttpStatusCode.InternalServerError, "Nå har det gått til skogen")
        }
    }
}
