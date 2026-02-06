package no.nav.helse.spiskammerset.spiskammerset

import com.auth0.jwk.JwkProvider
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kotlin.reflect.KClass
import no.nav.helse.spiskammerset.spiskammerset.Besøkende.Companion.besøkende

class AzureApp(
    private val jwkProvider: JwkProvider,
    private val issuer: String,
    private val clientId: String,
) {
    fun konfigurerJwtAuth(config: AuthenticationConfig) {
        config.autentiserBesøkende("spissmus", setOf(Besøkende.Spissmus::class, Besøkende.Spissmus.Hjelper::class))
        config.autentiserBesøkende("husmor", setOf(Besøkende.Husmor::class))
    }
    private fun AuthenticationConfig.autentiserBesøkende(authenticationName: String, tillatt: Set<KClass<out Besøkende>>) {
        jwt(authenticationName) {
            verifier(jwkProvider, issuer) {
                withAudience(clientId)
                withClaimPresence("azp_name")
            }
            validate { credentials ->
                val besøkende = credentials.besøkende()
                if (besøkende::class !in tillatt) throw Besøkende.Tyv(besøkende)
                besøkende
            }
        }
    }
}
