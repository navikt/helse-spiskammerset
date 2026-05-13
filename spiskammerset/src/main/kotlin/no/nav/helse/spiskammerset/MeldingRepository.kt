package no.nav.helse.spiskammerset

import java.time.ZoneOffset
import kotliquery.Session
import kotliquery.queryOf

interface MeldingRepository {
    fun lagre(dto: MeldingDto)
}

class MeldingRepositoryImpl(private val session: Session) : MeldingRepository {
    override fun lagre(dto: MeldingDto) {
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
