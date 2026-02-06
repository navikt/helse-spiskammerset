package no.nav.helse.spiskammerset.spiskammerset

import com.auth0.jwt.interfaces.Payload
import io.ktor.server.auth.jwt.JWTCredential
import kotlin.collections.contains

sealed interface Besøkende {
    val navn: String

    data class Spissmus(override val navn: String): Besøkende {
        data class Hjelper(val spissmus: Spissmus, override val navn: String): Besøkende
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

            val mellomnavn = payload.getClaim("name")?.asString()?.split(" ")?.lastOrNull() ?: throw Tyv(appnavn)
            val ident = payload.getClaim("NAVident")?.asString() ?: throw Tyv("$mellomnavn sendt fra $appnavn")

            return Spissmus.Hjelper(
                spissmus = Spissmus(appnavn),
                navn = "$mellomnavn ($ident)"
            )
        }
    }
}
