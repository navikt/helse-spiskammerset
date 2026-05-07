package no.nav.helse.spiskammerset.mottak

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import java.net.URI
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.forEachIndexed

internal fun JsonMessage.medFaktumIndexer(): JsonMessage {
    this["@løsning"].properties().forEach løsning@{ (_, løsning) ->
        if (løsning !is ObjectNode) return@løsning
        val fakta = løsning.path("fakta")
        if (fakta !is ArrayNode) return@løsning

        fakta.forEachIndexed faktum@{ index, faktum ->
            if (faktum !is ObjectNode) return@faktum
            faktum.put("@faktumIndex", index + 1)
        }
    }
    return this
}

private fun JsonNode.fjernFaktumIndexer() {
    if (!has("fakta")) return
    get("fakta").forEach { faktum ->
        if (faktum !is ObjectNode) return@forEach
        faktum.remove("@faktumIndex")
    }
}

internal fun JsonMessage.medFaktaIder(lagringIder: Map<String, URI>): JsonMessage {
    this["@løsning"].properties().forEach { (behovsnavn, løsning) ->
        val lagringId = lagringIder[behovsnavn] ?: return@forEach løsning.fjernFaktumIndexer()
        løsning as ObjectNode
        løsning.put("@faktaId", lagringId.toString())
    }
    return this
}
