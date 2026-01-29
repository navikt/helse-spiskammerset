package no.nav.helse.spiskammerset.mottak

import com.github.navikt.tbd_libs.azure.createAzureTokenClientFromEnvironment
import com.github.navikt.tbd_libs.kafka.AivenConfig
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import no.nav.helse.rapids_rivers.RapidApplication
import java.net.http.HttpClient

fun main() {
    val env = System.getenv()

    val spiskammersApiClient = SpiskammersApiClient(
        httpClient = HttpClient.newHttpClient(),
        azureTokenProvider = createAzureTokenClientFromEnvironment(env),
        env = env
    )

    val kafkaConfig = AivenConfig.default
    val consumerProducerFactory = ConsumerProducerFactory(kafkaConfig)

    RapidApplication.create(env, consumerProducerFactory = consumerProducerFactory).apply {
        ForsikringRiver(this, spiskammersApiClient)
    }.start()
}
