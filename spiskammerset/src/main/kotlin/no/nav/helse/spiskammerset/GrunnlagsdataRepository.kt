package no.nav.helse.spiskammerset

import org.intellij.lang.annotations.Language
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource

internal class GrunnlagsdataRepository(private val dataSource: DataSource) {
    fun lagre(dto: GrunnlagsdataDto) {
        @Language("SQL")
        val sql = "INSERT INTO grunnlagsdata (id, lagret_tidspunkt, data, type, melding_ref) VALUES (?, ?, ?::jsonb, ?, ?)"

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setObject(1, dto.id)
                stmt.setObject(2, dto.lagretTidspunkt.atOffset(ZoneOffset.UTC))
                stmt.setString(3, dto.data)
                stmt.setString(4, dto.type)
                stmt.setObject(5, dto.meldingRef)
                stmt.executeUpdate()
            }
        }
    }
}
