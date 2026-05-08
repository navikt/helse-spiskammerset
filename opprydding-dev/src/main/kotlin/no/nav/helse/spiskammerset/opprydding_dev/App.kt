package no.nav.helse.spiskammerset.opprydding_dev

import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val env = System.getenv()

    val dataSourceBuilder = DefaultDataSourceBuilder(env)
    val dataSource = dataSourceBuilder.getDataSource()

    RapidApplication.create(env).apply {
        SlettPersonRiver(this, dataSource)
    }.start()
}
