package no.nav.helse.spiskammerset.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory

internal class BenyttetGrunnlagsdataForBeregningRiver(
    rapidsConnection: RapidsConnection,
    private val spiskammersetKlient: SpiskammersetKlient
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireValue("@event_name", "benyttet_grunnlagsdata_for_beregning")
            }
            validate {
                it.requireKey("@id", "behandlingId", "fødselsnummer")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        sikkerlogg.info("Mottok opplysninger om benyttet grunnlagsdata for beregning:\n\t${packet.toJson()}")
        hendelse(packet)
    }

    private fun hendelse(packet: JsonMessage) = try {
        spiskammersetKlient.hendelse(packet)
    } catch (error: Exception) {
        sikkerlogg.error("Feil ved håndtering av benyttet grunnlagsdata for beregning", error)
        throw error
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        sikkerlogg.error("Forstod ikke benyttet grunnlagsdata for beregning:\n${problems.toExtendedReport()}")
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
