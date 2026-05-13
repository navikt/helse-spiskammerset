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

internal class LøsningContentEnricherRiver(
    rapidsConnection: RapidsConnection,
    private val dataSource: DataSource
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

    private val meldingRepository = MeldingRepository(dataSource)
    private val grunnlagsdataRepository = GrunnlagsdataRepository(dataSource)

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        teamLogs.info("Mottok komplett løsning på behov:\n\t${packet.toJson()}")

        val meldingId = UUID.fromString(packet["@id"].asText())
        meldingRepository.lagre(
            MeldingDto(
                id = meldingId,
                lagretTidspunkt = Instant.now(),
                data = packet.toJson()
            )
        )

        @OptIn(ExperimentalUuidApi::class)
        val uuid = Uuid.generateV7().toJavaUuid()
        val beriketLøsning = packet["@løsning"]["SelvstendigForsikring"] as ObjectNode
        beriketLøsning.put("@lagringsId", "urn:grunnlagsdata:forsikring:$uuid")
        beriketLøsning["kandidater"].forEachIndexed { index, element -> (element as ObjectNode).put("@index", index) }

        grunnlagsdataRepository.lagre(
            GrunnlagsdataDto(
                id = uuid,
                lagretTidspunkt = Instant.now(),
                data = beriketLøsning.toPrettyString(),
                type = "forsikring",
                meldingRef = meldingId
            )
        )

        packet["@lagret"] = true

        context.publish(packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        teamLogs.error("Forstod ikke komplett løsning på behov:\n${problems.toExtendedReport()}")
    }
}
