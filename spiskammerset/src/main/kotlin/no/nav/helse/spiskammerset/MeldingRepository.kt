package no.nav.helse.spiskammerset

import java.time.ZoneOffset
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf

internal class MeldingRepository(private val dataSource: DataSource) {
    fun lagre(dto: MeldingDto) {
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    // language=sql
                    "INSERT INTO melding" +
                        " (id, lagret_tidspunkt, data)" +
                        " VALUES" +
                        " (:id, :lagret_tidspunkt, :data::jsonb)",
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
