package no.nav.helse.spiskammerset.spiskammerset.rest

import com.github.navikt.tbd_libs.signed_jwt_issuer_test.Issuer
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.spiskammerset.spiskammerset.reisverk.BehandlingId
import org.intellij.lang.annotations.Language
import org.skyscreamer.jsonassert.JSONAssert
import java.util.*

internal data class RestApiTestContext(
    private val issuer: Issuer,
    private val client: HttpClient
) {
    internal fun spiskammersetMaskinAccessToken(rolle: String?) = issuer.accessToken {
        rolle?.let { withArrayClaim("roles", arrayOf(rolle, "${UUID.randomUUID()}")) }
        withClaim("azp_name", "${UUID.randomUUID()}")
        withClaim("idtyp", "app")
    }

    internal fun spiskammersetPersonToken(navn: String = "Nordmann, Ola", ident: String = "ABC123") = issuer.accessToken {
        withClaim("azp_name", "Spanner")
        withClaim("NAVident", ident)
        withClaim("name", navn)
    }

    suspend fun hentOpplysning(
        behandlingId: BehandlingId,
        accessToken: String = spiskammersetMaskinAccessToken("spissmus"),
        opplysning: String,
        assertResponse: (status: HttpStatusCode, respondeBody: String) -> Unit
    ) {
        val response = client.get("/behandling/$behandlingId/$opplysning") {
            header("Authorization", "Bearer $accessToken")
        }
        assertResponse(response.status, response.bodyAsText())
    }

    suspend fun hentOpplysninger(
        behandlingId: BehandlingId,
        accessToken: String = spiskammersetMaskinAccessToken("spissmus"),
        opplysninger: Set<String> = emptySet(),
        assertResponse: (status: HttpStatusCode, respondeBody: String) -> Unit
    ) {
        val response = client.get("/behandling/$behandlingId") {
            opplysninger.forEach { opplysning -> parameter("opplysning", opplysning) }
            header("Authorization", "Bearer $accessToken")
        }
        assertResponse(response.status, response.bodyAsText())
    }

    suspend fun lagreHendelse(
        jsonBody: String,
        accessToken: String = spiskammersetMaskinAccessToken("husmor"),
        assertResponse: (status: HttpStatusCode, responseBody: String) -> Unit
    ) {
        val response = client.post("/hendelse") {
            header("Authorization", "Bearer $accessToken")
            setBody(jsonBody)
        }
        assertResponse(response.status, response.bodyAsText())
    }

    fun assertJsonEquals(@Language("JSON") forventet: String, faktisk: String) = JSONAssert.assertEquals(forventet, faktisk, true)
}
