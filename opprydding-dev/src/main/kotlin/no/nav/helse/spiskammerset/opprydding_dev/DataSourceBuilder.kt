package no.nav.helse.spiskammerset.opprydding_dev

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import kotlin.getValue

internal interface DataSourceBuilder {
    val dataSource: DataSource
}

internal class DefaultDataSourceBuilder(env: Map<String, String>): DataSourceBuilder {

    private val baseConnectionConfig = HikariConfig().apply {
        jdbcUrl = env.getValue("DATABASE_SPISKAMMERSET_OPPRYDDING_DEV_URL")
    }

    private val appConfig = HikariConfig().apply {
        baseConnectionConfig.copyStateTo(this)
        maximumPoolSize = 2
    }

    override val dataSource by lazy { HikariDataSource(appConfig) }


    private companion object {
        private val logger = LoggerFactory.getLogger(DataSourceBuilder::class.java)
    }
}
