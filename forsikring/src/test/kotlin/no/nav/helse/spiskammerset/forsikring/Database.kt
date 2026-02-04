package no.nav.helse.spiskammerset.forsikring

import com.github.navikt.tbd_libs.test_support.CleanupStrategy
import com.github.navikt.tbd_libs.test_support.DatabaseContainers
import javax.sql.DataSource

val databaseContainer = DatabaseContainers.container("spiskammerset", CleanupStrategy.tables("forsikring"))
fun databaseTest(testblokk: (DataSource) -> Unit) {
    val testDataSource = databaseContainer.nyTilkobling()
    try {
        testblokk(testDataSource.ds)
    } finally {
        databaseContainer.droppTilkobling(testDataSource)
    }
}
