package no.nav.helse.spiskammerset

import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf

internal class GrunnlagsdataRepository(private val dataSource: DataSource) {
    fun lagre(dto: GrunnlagsdataDto) {
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    // language=sql
                    "INSERT INTO grunnlagsdata" +
                        " (id, lagret_tidspunkt, data, type, melding_ref)" +
                        " VALUES" +
                        " (:id, :lagret_tidspunkt, :data::jsonb, :type, :melding_ref)",
                    mapOf(
                        "id" to dto.id,
                        "lagret_tidspunkt" to dto.lagretTidspunkt,
                        "data" to dto.data,
                        "type" to dto.type,
                        "melding_ref" to dto.meldingRef,
                    )
                ).asUpdate
            )
        }
    }
}
