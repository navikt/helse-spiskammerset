package no.nav.helse.spiskammerset

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import java.time.Instant
import java.util.*
import javax.sql.DataSource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotliquery.sessionOf

internal class LøsningContentEnricherRiver(
    rapidsConnection: RapidsConnection,
    private val dataSource: DataSource,
    private val repositoryFactory: RepositoryFactory,
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireValue("@event_name", "behov")
                it.requireValue("@final", true) // Skal bare bry seg om komplette løsninger
                it.requireValue("@lagreLøsninger", true) // Spleis setter for å si at det er interessant for Spiskammerset, for at vi ikke blander med andre behov
                it.forbid("@lagret") // Trenger ikke lytte på events Spiskammerset selv sender ut
            }
            validate {
                it.requireKey("@id", "@løsning")
            }
        }.register(this)
    }

    val grunnlagsdataTypePerBehov = mapOf(
        "SelvstendigForsikring" to "forsikring"
    )

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val inputMelding = packet.toJson()
        teamLogs.info("Mottok komplett løsning på behov:\n\t$inputMelding")

        val meldingId = UUID.fromString(packet["@id"].asText())

        val grunnlagsdataDtoer = packet["@løsning"].properties()
            .mapNotNull { (behovsnavn, løsning) -> grunnlagsdataTypePerBehov[behovsnavn]?.let { løsning to it } }
            .map { (løsning, grunnlagsdataType) ->
                @OptIn(ExperimentalUuidApi::class)
                val uuid = Uuid.generateV7().toJavaUuid()
                val beriketLøsning = løsning as ObjectNode
                beriketLøsning.put("@lagringsId", "urn:grunnlagsdata:$grunnlagsdataType:$uuid")
                beriketLøsning["kandidater"]?.forEachIndexed { index, element -> (element as ObjectNode).put("@index", index) }

                GrunnlagsdataDto(
                    id = uuid,
                    lagretTidspunkt = Instant.now(),
                    data = beriketLøsning.toPrettyString(),
                    type = grunnlagsdataType,
                    meldingRef = meldingId
                )
            }

        if (grunnlagsdataDtoer.isNotEmpty()) {
            sessionOf(dataSource).use { session ->
                session.transaction { transactionalSession ->
                    val meldingRepository = repositoryFactory.meldingRepository(transactionalSession)
                    val grunnlagsdataRepository = repositoryFactory.grunnlagsdataRepository(transactionalSession)

                    meldingRepository.lagre(
                        MeldingDto(
                            id = meldingId,
                            lagretTidspunkt = Instant.now(),
                            data = inputMelding
                        )
                    )
                    grunnlagsdataDtoer.forEach { dto ->
                        grunnlagsdataRepository.lagre(dto)
                    }
                }
            }
        }

        packet["@lagret"] = true

        context.publish(packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        teamLogs.error("Forstod ikke komplett løsning på behov:\n${problems.toExtendedReport()}")
    }
}
