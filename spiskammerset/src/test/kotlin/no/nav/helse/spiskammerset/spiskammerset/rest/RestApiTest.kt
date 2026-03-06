package no.nav.helse.spiskammerset.spiskammerset.rest

import com.github.navikt.tbd_libs.naisful.test.naisfulTestApp
import com.github.navikt.tbd_libs.signed_jwt_issuer_test.Issuer
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.spiskammerset.oppbevaringsboks.Oppbevaringsboks
import no.nav.helse.spiskammerset.spiskammerset.databaseContainer
import no.nav.helse.spiskammerset.spiskammerset.db.DataSourceBuilder
import no.nav.helse.spiskammerset.spiskammerset.objectmapper
import no.nav.helse.spiskammerset.spiskammerset.spiskammerset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

@TestInstance(Lifecycle.PER_METHOD)
internal abstract class RestApiTest(
    private val oppbevaringsbokser: List<Oppbevaringsboks> = listOf(
        TestOppbevaringsboks(eventName = "test_event_1", etikett = "info1"),
        TestOppbevaringsboks(eventName = "test_event_2", etikett = "info2"),
        TestOppbevaringsboks(eventName = "test_event_3", etikett = "info3")
    )
) {

    private val issuer = Issuer("lokal", "http://audience")
    private val testDataSource by lazy { databaseContainer.nyTilkobling() }

    @BeforeEach
    fun setup() {
        issuer.start()
    }
    @AfterEach
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
                    ),
                    oppbevaringsbokser = oppbevaringsbokser
                )
            },
            objectMapper = objectmapper,
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            testblokk = {
                testblokk(RestApiTestContext(issuer, client))
            }
        )
    }
}
