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

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        teamLogs.info("Mottok komplett løsning på behov:\n\t${packet.toJson()}")

        meldingRepository.lagre(
            MeldingDto(
                id = UUID.fromString(packet["@id"].asText()),
                lagretTidspunkt = Instant.now(),
                data = packet.toJson()
            )
        )

        @OptIn(ExperimentalUuidApi::class)
        val uuid = Uuid.generateV7().toJavaUuid()
        (packet["@løsning"]["SelvstendigForsikring"] as ObjectNode).put("@lagringsId", "urn:grunnlagsdata:forsikring:$uuid")
        packet["@løsning"]["SelvstendigForsikring"]["kandidater"].forEachIndexed { index, element -> (element as ObjectNode).put("@index", index) }
        packet["@lagret"] = true

        context.publish(packet.toJson())
        /*
        val navnPåLøsningerSomSkalLagres = mapOf("SelvstendigForsikring" to "forsikring")

        val løsningerSomSkalLagres = navnPåLøsningerSomSkalLagres
            .mapNotNull { (navnPåLøsningSomSkalLagres, navnIIdPåLøsningSomSkalLagres) -> navnIIdPåLøsningSomSkalLagres to packet["@løsning"][navnPåLøsningSomSkalLagres] }
            .onEach { (navnIIdPåLøsningSomSkalLagres, løsning) ->
                if (løsning is ObjectNode) {
                    løsning["@lagretId"] = "urn:grunnlagsdata:${navnIIdPåLøsningSomSkalLagres}:${UUID.randomUUID()}"
                }
            }

        packet["@løsning"].properties().forEach løsning@{ (_, løsning) ->
            if (løsning !is ObjectNode) return@løsning
            val kandidater = løsning.path("kandidater")
            if (kandidater !is ArrayNode) return@løsning

            kandidater.forEachIndexed faktum@{ index, faktum ->
                if (faktum !is ObjectNode) return@faktum
                faktum.put("@kandidatIndex", index + 1)
            }
        }

        try {
            when (val lagringsresultat = spiskammersetKlient.lagreLøsninger(packet)) {
                Lagringsresultat.LagretTidligere -> teamLogs.info("Ignorerer melding. Håndtert den tidligere")
                is Lagringsresultat.LagretNå -> {

                    packet["@løsning"].properties().forEach { (behovsnavn, løsning) ->
                        val lagringId = lagringsresultat.lagringIder[behovsnavn] ?: return@forEach løsning.fjernFaktumIndexer()
                        check(løsning is ObjectNode) { "Støtter bare å lagre løsninger som er JSON-objekter, var ${løsning::class.simpleName}" }
                        løsning.put("@faktaId", lagringId.toString())
                    }
                    val medFaktaIder = packet
                    medFaktaIder["@lagret"] = true
                    val enriched = medFaktaIder.toJson()

                    when (lagringsresultat.lagringIder.isEmpty()) {
                        true -> teamLogs.info("Republiserer komplett løsning på behov uten å lagre ned noen løsninger:\n\t${enriched}")
                        false -> teamLogs.info("Republiserer komplett løsning på behov hvor ${lagringsresultat.lagringIder.size} løsninger har blitt lagret ned (${lagringsresultat.lagringIder.keys.joinToString()}):\n\t${enriched}")
                    }

                    context.publish(enriched)
                }
            }
        } catch (error: Exception) {
            teamLogs.error("Feil ved håndtering av komplett løsning", error)
            throw error
        }
         */
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        teamLogs.error("Forstod ikke komplett løsning på behov:\n${problems.toExtendedReport()}")
    }
}
