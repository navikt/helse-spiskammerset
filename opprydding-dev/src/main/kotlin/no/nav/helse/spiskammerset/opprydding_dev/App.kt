package no.nav.helse.spiskammerset.opprydding_dev

import com.github.navikt.tbd_libs.kafka.AivenConfig
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import no.nav.helse.rapids_rivers.RapidApplication


fun main() {
    val env = System.getenv()

    val kafkaConfig = AivenConfig.default
    val consumerProducerFactory = ConsumerProducerFactory(kafkaConfig)
    val dataSourceBuilder = DefaultDataSourceBuilder(env)
    val dataSource = dataSourceBuilder.dataSource


    RapidApplication.create(env, consumerProducerFactory = consumerProducerFactory).apply {
        SlettPersonRiver(this, dataSource)
    }.start()
}

