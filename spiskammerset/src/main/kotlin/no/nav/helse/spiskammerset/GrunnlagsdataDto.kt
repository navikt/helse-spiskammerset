package no.nav.helse.spiskammerset

import java.time.Instant
import java.util.*
import kotlin.uuid.ExperimentalUuidApi

data class GrunnlagsdataDto @OptIn(ExperimentalUuidApi::class) constructor(
    val id: UUID,
    val lagretTidspunkt: Instant,
    val data: String,
    val type: String,
    val meldingRef: UUID,
)
