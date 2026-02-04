package no.nav.helse.spiskammerset.spiskammerset

import io.ktor.http.*
import no.nav.helse.spiskammerset.spiskammerset.reisverk.BehandlingId
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

internal class ApiTest : RestApiTest() {

    @Test
    fun `prøver å hente forsikring for behandling som ikke har forsikring`() = spiskammersetTestApp {
        val id = UUID.randomUUID()
        hentForsikring(
            behandlingId = BehandlingId(id),
            assertResponse = { status, responseBody ->
                assertEquals(HttpStatusCode.NotFound, status)
                assertJsonEquals("""{ "feilmelding": "Fant ikke forsikring for behandlingId: $id" }""", responseBody)
            }
        )
    }
}