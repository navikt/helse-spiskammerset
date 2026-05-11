package no.nav.helse.spiskammerset.opprydding_dev

import io.ktor.server.application.ApplicationStopped
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    RapidApplication.create(
        env = System.getenv(),
        builder = {
            withKtorModule {
                monitor.subscribe(ApplicationStopped) {
                    EnvBasedDataSourceProvider.dataSource
                }
            }
        }
    ).apply {
        SlettPersonRiver(this, EnvBasedDataSourceProvider.dataSource)
    }.start()
}
