package no.nav.helse.spiskammers.mottak

import com.fasterxml.jackson.databind.JsonNode
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

internal class SpiskammersApiClient(
    private val httpClient: HttpClient,
    private val azureTokenProvider: AzureTokenProvider,
    env: Map<String, String>
) {
    private val cluster = env["NAIS_CLUSTER_NAME"]?.lowercase() ?: "prod-gcp"
    private val scope = "api://$cluster.tbd.spiskammers-api/.default"

    fun forsikring(packet: JsonMessage) {
        post(
            endepunkt = "forsikring",
            requestBody = packet.toJson(),
            callId = UUID.fromString(packet["@id"].asText()),
            forventetResponseCode = 201
        )
    }

    private fun post(endepunkt: String, requestBody: String, callId: UUID, forventetResponseCode: Int): JsonNode {
        val accessToken = azureTokenProvider.bearerToken(scope).getOrThrow()
        val request = HttpRequest
            .newBuilder()
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .uri(URI("http://spiskammers-api/$endepunkt"))
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${accessToken.token}")
            .header("callId", "$callId")
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() == forventetResponseCode) {
            "Feil fra Spiskammers-API. Forventet HTTP $forventetResponseCode, men fikk ${response.statusCode()}"
        }
        return objectmapper.readTree(response.body())
    }

    private companion object {
        private val objectmapper = jacksonObjectMapper()
    }
}
