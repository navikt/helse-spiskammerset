package no.nav.helse.spiskammerset

import kotliquery.Session

interface RepositoryFactory {
    fun meldingRepository(session: Session): MeldingRepository
    fun grunnlagsdataRepository(session: Session): GrunnlagsdataRepository
}

class RepositoryFactoryImpl : RepositoryFactory {
    override fun meldingRepository(session: Session) = MeldingRepositoryImpl(session)
    override fun grunnlagsdataRepository(session: Session) = GrunnlagsdataRepositoryImpl(session)
}
