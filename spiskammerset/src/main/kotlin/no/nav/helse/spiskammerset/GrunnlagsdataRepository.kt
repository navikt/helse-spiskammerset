package no.nav.helse.spiskammerset

import java.time.ZoneOffset
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language

internal class GrunnlagsdataRepository(private val dataSource: DataSource) {
    fun lagre(dto: GrunnlagsdataDto) {
        @Language("SQL")
        val sql = "INSERT INTO grunnlagsdata (id, lagret_tidspunkt, data, type, melding_ref) VALUES (:id, :lagret_tidspunkt, :data::jsonb, :type, :melding_ref)"

        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    sql,
                    mapOf(
                        "id" to dto.id,
                        "lagret_tidspunkt" to dto.lagretTidspunkt.atOffset(ZoneOffset.UTC),
                        "data" to dto.data,
                        "type" to dto.type,
                        "melding_ref" to dto.meldingRef,
                    )
                ).asUpdate
            )
        }
    }
}
