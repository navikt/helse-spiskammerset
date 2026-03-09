package no.nav.helse.spiskammerset.mottak

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory

internal class LøsningContentEnricherRiver(
    rapidsConnection: RapidsConnection,
    private val spiskammersetKlient: SpiskammersetKlient
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireValue("@event_name", "behov")
                it.requireValue("@final", true) // Skal bare bry seg om komplette løsninger
                it.requireValue("@lagreLøsninger", true) // Spleis setter for å si at det er interessant for Spiskammerset, for at vi ikke blander med andre behov
                it.forbidValue("@lagret", true) // Trenger ikke lytte på event Spiskammerset sender ut
            }
            validate {
                it.requireKey("@id", "fødselsnummer", "@løsning")
                it.interestedIn("beregningId", "behandlingId", "vedtaksperiodeId")
                // TODO noe med samlingVurderteVilkårId? SkjæringstidspunktavgjørelserId? SkjæringstidspunktAvgjørelserId??
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        sikkerlogg.info("Mottok komplett løsning på behov:\n\t${packet.toJson()}")
        when (val lagringsresultat = lagreLøsninger(packet)) {
            Lagringsresultat.LagretTidligere -> sikkerlogg.info("Ignorerer melding. Håndtert den tidligere")
            is Lagringsresultat.LagretNå -> {

                val løsningMedLagringIder = jacksonObjectMapper().createObjectNode()
                packet["@løsning"].properties().forEach { (behovsnavn, løsning) ->
                    val lagringId = lagringsresultat.lagringIder[behovsnavn]
                    if (lagringId == null) {
                        løsningMedLagringIder.set<JsonNode>(behovsnavn, løsning)
                    } else {
                        val løsningMedLagringId = (løsning as ObjectNode).put("lagringId", lagringId.toString())
                        løsningMedLagringIder.set(behovsnavn, løsningMedLagringId)
                    }
                }
                packet["@løsning"] = løsningMedLagringIder
                packet["@lagret"] = true
                val enriched = packet.toJson()

                if (lagringsresultat.lagringIder.size == 0) sikkerlogg.info("Republiserer komplett løsning på behov uten å lagre ned noen løsninger:\n\t${enriched}")
                else sikkerlogg.info("Republiserer komplett løsning på behov hvor ${lagringsresultat.lagringIder.size} løsninger har blitt lagret ned (${lagringsresultat.lagringIder.keys.joinToString()}):\n\t${enriched}")

                context.publish(enriched)
            }
        }
    }

    private fun lagreLøsninger(packet: JsonMessage) = try {
        spiskammersetKlient.lagreLøsninger(packet)
    } catch (error: Exception) {
        sikkerlogg.error("Feil ved håndtering av komplett løsning", error)
        throw error
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        sikkerlogg.error("Forstod ikke komplett løsning:\n${problems.toExtendedReport()}")
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
