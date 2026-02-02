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
                withArrayClaim("roles", rolle)
            }
            validate { credentials ->
                JWTPrincipal(credentials.payload)
            }
        }
    }
}
