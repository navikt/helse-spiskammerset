package no.nav.helse.spiskammerset

import java.time.Instant
import java.util.UUID

data class MeldingDto(
    val id: UUID,
    val lagretTidspunkt: Instant,
    val data: String,
)
