package no.nav.helse.spiskammerset.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory

internal class BehandlingOpprettetRiver(
    rapidsConnection: RapidsConnection,
    private val spiskammersetKlient: SpiskammersetKlient
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireValue("@event_name", "behandling_opprettet")
            }
            validate {
                it.requireKey("@id", "behandlingId", "fødselsnummer", "vedtaksperiodeId", "yrkesaktivitetstype", "fom", "tom", "@opprettet")
                it.interestedIn("organisasjonsnummer")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        sikkerlogg.info("Mottok behandling opprettet:\n\t${packet.toJson()}")
        hendelse(packet)
    }

    private fun hendelse(packet: JsonMessage) = try {
        spiskammersetKlient.hendelse(packet)
    } catch (error: Exception) {
        sikkerlogg.error("Feil ved håndtering av behandling opprettet", error)
        throw error
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        sikkerlogg.error("Forstod ikke behandling opprettet:\n${problems.toExtendedReport()}")
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
