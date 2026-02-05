package no.nav.helse.spiskammerset.spiskammerset

import com.auth0.jwk.JwkProvider
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

class AzureApp(
    private val jwkProvider: JwkProvider,
    private val issuer: String,
    private val clientId: String,
) {
    fun konfigurerJwtAuth(config: AuthenticationConfig) {
        config.autentiserRolle("spissmus")
        config.autentiserRolle("husmor")
    }
    private fun AuthenticationConfig.autentiserRolle(rolle: String) {
        jwt(rolle) {
            verifier(jwkProvider, issuer) {
                withAudience(clientId)
                withClaimPresence("azp_name")
                withClaim("azp_name") { _, jwt ->
                    if (jwt.claims["idtyp"]?.asString() == "app") {
                        rolle in (jwt.claims["roles"]?.asArray(String::class.java) ?: emptyArray<String>())
                    } else {
                        "c0227409-2085-4eb2-b487-c4ba270986a3" in (jwt.claims["groups"]?.asArray(String::class.java) ?: emptyArray<String>()) // TODO burde vi ha noe gruppesjekk her
                    }
                }
            }
            validate { credentials ->
                JWTPrincipal(credentials.payload)
            }
        }
    }
}
