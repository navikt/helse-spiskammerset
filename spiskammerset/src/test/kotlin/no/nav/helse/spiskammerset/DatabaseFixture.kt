package no.nav.helse.spiskammerset

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import org.flywaydb.core.Flyway
import org.testcontainers.postgresql.PostgreSQLContainer

object DatabaseFixture {
    private val postgres = PostgreSQLContainer("postgres:18").also { it.start() }
    val dataSource: HikariDataSource =
        HikariDataSource(HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
            maximumPoolSize = 2
        }).also {
            Flyway.configure()
                .dataSource(it)
                .locations("classpath:db/migration")
                .load()
                .migrate()
        }

    fun reset() {
        sessionOf(dataSource).use { session ->
            session.run(queryOf("TRUNCATE TABLE grunnlagsdata, melding").asUpdate)
        }
    }

    fun fetchMeldingData(meldingId: UUID): String? =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "SELECT data FROM melding WHERE id = :id",
                    mapOf("id" to meldingId)
                ).map { row -> row.string(1) }.asSingle
            )
        }

    fun fetchGrunnlagsdata(id: String, type: String): String? =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "SELECT data FROM grunnlagsdata WHERE id = :id::uuid AND type = :type",
                    mapOf("id" to id, "type" to type)
                ).map { row -> row.string(1) }.asSingle
            )
        }

    fun fetchMeldingRefForGrunnlagsdata(id: String, type: String): String? =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "SELECT melding_ref FROM grunnlagsdata WHERE id = :id::uuid AND type = :type",
                    mapOf("id" to id, "type" to type)
                ).map { row -> row.string(1) }.asSingle
            )
        }

    fun countGrunnlagsdataRows(): Int = countTableRows("grunnlagsdata")

    fun countMeldingRows(): Int = countTableRows("melding")

    private fun countTableRows(table: String): Int =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf("SELECT COUNT(*) FROM $table").map { row -> row.int(1) }.asSingle
            ) ?: 0
        }
}
