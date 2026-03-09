package no.nav.helse.spiskammerset.mottak

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.azure.AzureTokenProvider
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.result_object.getOrThrow
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.*

sealed interface Lagringsresultat {
    data object LagretTidligere : Lagringsresultat
    data class LagretNå(val lagringIder: Map<String, URI>) : Lagringsresultat
}

interface SpiskammersetKlient {
    fun lagreLøsninger(packet: JsonMessage): Lagringsresultat
}

internal class RestSpiskammersetKlient(
    private val httpClient: HttpClient,
    private val azureTokenProvider: AzureTokenProvider,
    env: Map<String, String>
) : SpiskammersetKlient {
    private val cluster = env["NAIS_CLUSTER_NAME"]?.lowercase() ?: "prod-gcp"
    private val scope = "api://$cluster.tbd.spiskammerset/.default"

    fun hendelse(packet: JsonMessage) {
        post(
            endepunkt = "hendelse",
            requestBody = packet.toJson(),
            callId = UUID.fromString(packet["@id"].asText()),
            forventetResponseCodes = listOf(204)
        )
    }

    override fun lagreLøsninger(packet: JsonMessage): Lagringsresultat {
        val (responseBody, responseCode) = post(
            endepunkt = "lagre-losninger",
            requestBody = packet.toJson(),
            callId = UUID.fromString(packet["@id"].asText()),
            forventetResponseCodes = listOf(200, 201)
        )

        if (responseCode == 200) return Lagringsresultat.LagretTidligere

        val lagredeLøsningIder = responseBody.path("lagringIder") as ObjectNode
        return Lagringsresultat.LagretNå(lagredeLøsningIder.properties().associate { (behovsnavn, urn) ->
            behovsnavn to URI(urn.asText())
        })
    }

    private data class Response(val responseBody: JsonNode, val responseCode: Int)

    private fun post(endepunkt: String, requestBody: String, callId: UUID, forventetResponseCodes: List<Int>): Response {
        val accessToken = azureTokenProvider.bearerToken(scope).getOrThrow()
        val request = HttpRequest
            .newBuilder()
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .uri(URI("http://spiskammerset/$endepunkt"))
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${accessToken.token}")
            .header("callId", "$callId")
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() in forventetResponseCodes) {
            "Feil fra Spiskammers-API. Forventet HTTP $forventetResponseCodes, men fikk ${response.statusCode()}"
        }
        return Response(objectmapper.readTree(response.body()), response.statusCode())
    }

    private companion object {
        private val objectmapper = jacksonObjectMapper()
    }
}
