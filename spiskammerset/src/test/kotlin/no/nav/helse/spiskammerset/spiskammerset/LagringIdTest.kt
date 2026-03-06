package no.nav.helse.spiskammerset.spiskammerset

import no.nav.helse.spiskammerset.spiskammerset.reisverk.LagringId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URI
import java.net.URISyntaxException
import kotlin.test.assertEquals

internal class LagringIdTest {

    @Test
    fun `Parser en gyldig lagringId`() {
        val lagringId = LagringId(URI("urn:grunnlagsdata:opplysning:00000000-0000-0000-0000-000000000001"))

        assertEquals("opplysning", lagringId.etikett)
        assertEquals("00000000-0000-0000-0000-000000000001", lagringId.id)
    }

    @Test
    fun `Parser en lagringId med ugyldig scheme`() {
        assertThrows<IllegalArgumentException> { LagringId(URI("url:grunnlagsdata:opplysning:00000000-0000-0000-0000-000000000001")) }
    }

    @Test
    fun `Parser en lagringId med ugyldige biter`() {
        assertThrows<IllegalArgumentException> { LagringId(URI("urn:grunnlagsdata:opplysning:00000000-0000-0000-0000-000000000001:tull")) }
        assertThrows<IllegalArgumentException> { LagringId(URI("urn:grunnlagsdata:opplysning")) }
        assertThrows<IllegalArgumentException> { LagringId(URI("urn:grunnlagsdata:opplysning:")) }
        assertThrows<IllegalArgumentException> { LagringId(URI("urn:grunnlagsdata::")) }
        assertThrows<URISyntaxException> { LagringId(URI("urn:grunnlagsdata: : ")) }
        assertThrows<URISyntaxException> { LagringId(URI(":grunnlagsdata:opplysning:Id")) }
    }
}