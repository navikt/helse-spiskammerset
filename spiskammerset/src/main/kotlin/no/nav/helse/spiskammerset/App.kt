package no.nav.helse.spiskammerset

import com.auth0.jwk.JwkProviderBuilder
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.uri
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import java.net.URI
import java.util.*
import javax.sql.DataSource
import no.nav.helse.rapids_rivers.RapidApplication
import org.flywaydb.core.Flyway
import org.slf4j.event.Level

fun main() {
    val env = System.getenv()

    val dataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = env.getValue("DATABASE_JDBC_URL")
            maximumPoolSize = 2
        }
    )

    RapidApplication.create(
        env,
        builder = {
            withKtorModule {
                spiskammerset(env, dataSource)
            }
        }
    ).apply {
        LøsningContentEnricherRiver(this, dataSource)
    }.start()
}

internal fun Application.spiskammerset(
    env: Map<String, String>,
    dataSource: DataSource,
) {
    val clientId = env.getValue("AZURE_APP_CLIENT_ID")
    val issuerUrl = env.getValue("AZURE_OPENID_CONFIG_ISSUER")
    val jwkProviderUri = env.getValue("AZURE_OPENID_CONFIG_JWKS_URI")

    install(CallId) {
        retrieveFromHeader(HttpHeaders.XRequestId)
        generate { UUID.randomUUID().toString() }
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            teamLogs.error("Unhandled: ${call.request.httpMethod.value} ${call.request.uri} - $cause")
            call.respondText(text = "Det skjedde en uventet feil", status = HttpStatusCode.InternalServerError)
        }
    }
    install(CallLogging) {
        disableDefaultColors()
        logger = teamLogs
        level = Level.INFO
        callIdMdc("callId")
        filter { call -> call.request.path() !in setOf("/metrics", "/isalive", "/isready") }
    }
    install(ContentNegotiation) { jackson() }

    authentication {
        jwt("jwt") {
            verifier(
                jwkProvider = JwkProviderBuilder(URI(jwkProviderUri).toURL()).build(),
                issuer = issuerUrl,
            ) {
                withAudience(clientId)
            }
            validate { credentials -> JWTPrincipal(credentials.payload) }
        }
    }

    monitor.subscribe(ApplicationStarted) {
        logg.info("Migrerer database")
        Flyway.configure()
            .dataSource(dataSource)
            .cleanDisabled(false)
            .lockRetryCount(-1)
            .load()
            .also { it.clean() }
            .migrate()
        logg.info("Migrering ferdig!")
    }

    routing {
        authenticate("jwt") {
            hentLøsningerApi(dataSource)
        }
    }
}
