package no.nav.helse.spiskammerset

import java.time.ZoneOffset
import org.intellij.lang.annotations.Language
import org.postgresql.util.PGobject
import javax.sql.DataSource

internal class MeldingRepository(private val dataSource: DataSource) {
    fun lagre(dto: MeldingDto) {
        @Language("SQL")
        val sql = "INSERT INTO melding (id, lagret_tidspunkt, data) VALUES (?, ?, ?::jsonb)"

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setObject(1, dto.id)
                stmt.setObject(2, dto.lagretTidspunkt.atOffset(ZoneOffset.UTC))
                stmt.setString(3, dto.data)
            }
        }
    }
}
