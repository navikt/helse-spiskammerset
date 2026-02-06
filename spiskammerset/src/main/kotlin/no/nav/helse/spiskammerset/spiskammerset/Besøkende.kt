package no.nav.helse.spiskammerset.spiskammerset

import com.auth0.jwt.interfaces.Payload
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.principal
import io.ktor.server.plugins.calllogging.processingTimeMillis
import io.ktor.server.request.path
import kotlin.collections.contains

sealed interface Besøkende {
    val navn: String

    data class Spissmus(override val navn: String): Besøkende {
        data class Hjelper(val spissmus: Spissmus): Besøkende {
            override val navn: String get() = error("Det er hemmelig!!! 100% hemmelig!!")
        }
    }
    data class Husmor(override val navn: String): Besøkende
    data class Tyv(override val navn: String): Besøkende, Throwable() {
        constructor(besøkende: Besøkende): this(besøkende.navn)
    }

    companion object {
        private fun Payload.getListe(claimNavn: String) = getClaim(claimNavn).asArray(String::class.java) ?: emptyArray()

        fun JWTCredential.besøkende(): Besøkende {
            val appnavn = checkNotNull(payload.getClaim("azp_name")?.asString())

            if (payload.getClaim("idtyp")?.asString() == "app") {
                val roller = payload.getListe("roles")
                return when {
                    "husmor" in roller -> Husmor(appnavn)
                    "spissmus" in roller -> Spissmus(appnavn)
                    else -> throw Tyv(appnavn)
                }
            }

            payload.getClaim("name")?.asString()?.split(" ")?.lastOrNull() ?: throw Tyv("En 100% anonym tyv sendt fra $appnavn")
            payload.getClaim("NAVident")?.asString() ?: throw Tyv("En 100% anonym tyv sendt fra $appnavn")

            return Spissmus.Hjelper(spissmus = Spissmus(appnavn))
        }
    }
}

internal val Gjestebok =
    createRouteScopedPlugin("Gjestebok") {
        onCallRespond { call ->
            val besøkende = call.principal<Besøkende>() ?: return@onCallRespond
            val tidsbruk = call.processingTimeMillis()

            val denBesøkende = when (besøkende) {
                is Besøkende.Husmor -> "Husmoren ${besøkende.navn}"
                is Besøkende.Spissmus -> "Spissmusen ${besøkende.navn}"
                is Besøkende.Spissmus.Hjelper -> "Spissmusen ${besøkende.spissmus.navn} sin 100% anonyme hjelper"
                is Besøkende.Tyv -> throw besøkende
            }

            val gjøremål = when (besøkende) {
                is Besøkende.Husmor -> "legge noe på hyllene"
                is Besøkende.Spissmus,
                is Besøkende.Spissmus.Hjelper -> {
                    val opplysninger = call.request.queryParameters.getAll("opplysning")?.takeUnless { it.isEmpty() }?.let { " (${it.joinToString()})" } ?: ""
                    "hente ${call.request.path()}${opplysninger}"
                }
                is Besøkende.Tyv -> throw besøkende
            }

            sikkerlogg.info("Nå var $denBesøkende innom i ${tidsbruk}ms for å $gjøremål")
        }
    }
