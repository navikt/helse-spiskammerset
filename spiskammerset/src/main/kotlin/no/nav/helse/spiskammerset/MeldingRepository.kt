package no.nav.helse.spiskammerset

import java.time.ZoneOffset
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language

internal class MeldingRepository(private val dataSource: DataSource) {
    fun lagre(dto: MeldingDto) {
        @Language("SQL")
        val sql = "INSERT INTO melding (id, lagret_tidspunkt, data) VALUES (:id, :lagret_tidspunkt, :data::jsonb)"

        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    sql,
                    mapOf(
                        "id" to dto.id,
                        "lagret_tidspunkt" to dto.lagretTidspunkt.atOffset(ZoneOffset.UTC),
                        "data" to dto.data,
                    )
                ).asUpdate
            )
        }
    }
}
