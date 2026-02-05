package no.nav.helse.spiskammerset.spiskammerset.rest

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.naisful.test.naisfulTestApp
import com.github.navikt.tbd_libs.signed_jwt_issuer_test.Issuer
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.spiskammerset.spiskammerset.databaseContainer
import no.nav.helse.spiskammerset.spiskammerset.db.DataSourceBuilder
import no.nav.helse.spiskammerset.spiskammerset.spiskammerset
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS

@TestInstance(PER_CLASS)
internal abstract class RestApiTest {

    private val issuer = Issuer("lokal", "http://audience")
    private val testDataSource by lazy { databaseContainer.nyTilkobling() }

    @BeforeAll
    fun setup() {
        issuer.start()
    }
    @AfterAll
    fun teardown() {
        issuer.stop()
        databaseContainer.droppTilkobling(testDataSource)
    }

    protected fun restApiTest(testblokk: suspend RestApiTestContext.() -> Unit) {
        naisfulTestApp(
            testApplicationModule = {
                spiskammerset(
                    dataSourceBuilder = object : DataSourceBuilder {
                        override val dataSource by lazy { testDataSource.ds }
                        override fun migrate() {}
                    },
                    env = mapOf(
                        "AZURE_OPENID_CONFIG_JWKS_URI" to "${issuer.jwksUri()}",
                        "AZURE_OPENID_CONFIG_ISSUER" to issuer.navn,
                        "AZURE_APP_CLIENT_ID" to issuer.audience
                    )
                )
            },
            objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule()),
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            testblokk = {
                testblokk(RestApiTestContext(issuer, client))
            }
        )
    }
}
