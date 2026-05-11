package no.nav.helse.spiskammerset

import java.time.Instant
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal data class GrunnlagsdataDto @OptIn(ExperimentalUuidApi::class) constructor(
    val id: UUID = Uuid.generateV7().toJavaUuid(),
    val lagretTidspunkt: Instant,
    val data: String,
    val type: String,
    val meldingRef: UUID,
)
