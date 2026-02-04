package no.nav.helse.spiskammerset.spiskammerset

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
    fun spiskammersetAccessToken(rolle: String?) = issuer.accessToken {
        rolle?.let { withArrayClaim("roles", arrayOf(rolle, "${UUID.randomUUID()}")) }
        withClaim("azp_name", "${UUID.randomUUID()}")
    }
    suspend fun hentForsikring(
        behandlingId: BehandlingId,
        accessToken: String = spiskammersetAccessToken("spissmus"),
        assertResponse: (status: HttpStatusCode, respondeBody: String) -> Unit
    ) {
        val response = client.get("/behandling/$behandlingId/forsikring") {
            header("Authorization", "Bearer $accessToken")
        }
        assertResponse(response.status, response.bodyAsText())
    }

    fun assertJsonEquals(@Language("JSON") forventet: String, faktisk: String) = JSONAssert.assertEquals(forventet, faktisk, true)
}
