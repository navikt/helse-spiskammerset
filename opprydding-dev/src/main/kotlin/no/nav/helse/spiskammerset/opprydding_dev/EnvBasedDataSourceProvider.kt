package no.nav.helse.spiskammerset.opprydding_dev

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.time.Duration
import javax.sql.DataSource
import org.slf4j.LoggerFactory

object EnvBasedDataSourceProvider {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private fun Map<String, String>.getRequired(value: String) = requireNotNull(get(value)) { "$value must be set" }

    private val hikariConfig = HikariConfig().apply {
        val env = System.getenv()

        jdbcUrl = "jdbc:postgresql:///${env.getRequired("DATABASE_SPISKAMMERSET_OPPRYDDING_DEV_DATABASE")}" +
            "?cloudSqlInstance=${env.getRequired("GCP_TEAM_PROJECT_ID")}:${env.getRequired("DATABASE_REGION")}:${env.getRequired("DATABASE_INSTANCE")}" +
            "&socketFactory=com.google.cloud.sql.postgres.SocketFactory"

        username = env.getRequired("DATABASE_SPISKAMMERSET_OPPRYDDING_DEV_USERNAME")
        password = env.getRequired("DATABASE_SPISKAMMERSET_OPPRYDDING_DEV_PASSWORD")

        maximumPoolSize = 3
        minimumIdle = 1
        initializationFailTimeout = Duration.ofMinutes(1).toMillis()
        connectionTimeout = Duration.ofSeconds(5).toMillis()
        maxLifetime = Duration.ofMinutes(30).toMillis()
        idleTimeout = Duration.ofMinutes(10).toMillis()
    }

    private val _dataSource by lazy { HikariDataSource(hikariConfig) }
    val dataSource: DataSource
        get() = _dataSource

    fun close() {
        logger.info("Forsøker å lukke datasource...")
        _dataSource.close()
        logger.info("Lukket datasource")
    }
}
