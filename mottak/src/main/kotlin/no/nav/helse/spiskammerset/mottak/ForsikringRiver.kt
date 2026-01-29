package no.nav.helse.spiskammerset.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory

internal class ForsikringRiver(
    rapidsConnection: RapidsConnection,
    private val spiskammersApiClient: SpiskammersApiClient
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireValue("@event_name", "benyttet_grunnlagsdata_for_beregning")
                it.requireKey("forsikring")
            }
            validate {
                it.requireKey("@id", "behandlingId", "forsikring.dekningsgrad", "forsikring.navOvertarAnsvarForVentetid")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        sikkerlogg.info("Mottok opplysninger om forsikring:\n\t${packet.toJson()}")
        forsikring(packet)
    }

    private fun forsikring(packet: JsonMessage) = try {
        spiskammersApiClient.forsikring(packet)
    } catch (err: Exception) {
        sikkerlogg.error("Feil ved håndtering av forsikring", err)
        throw err
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        sikkerlogg.error("Forstod ikke forsikring:\n${problems.toExtendedReport()}")
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
