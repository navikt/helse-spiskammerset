package no.nav.helse.spiskammerset

import kotliquery.Session
import kotliquery.queryOf

interface GrunnlagsdataRepository {
    fun lagre(dto: GrunnlagsdataDto)
}

class GrunnlagsdataRepositoryImpl(private val session: Session) : GrunnlagsdataRepository {
    override fun lagre(dto: GrunnlagsdataDto) {
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
