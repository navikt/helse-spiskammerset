package no.nav.helse.spiskammerset.spiskammerset

import java.util.UUID
import kotlin.test.assertEquals
import no.nav.helse.spiskammerset.oppbevaringsboks.Oppbevaringsboks.Companion.valider
import no.nav.helse.spiskammerset.spiskammerset.rest.BehovLøsningTestOppbevaringsboks
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class OppevaringsboksTest {

    @Test
    fun `gylidge oppbevaringsbokser`() {
        assertDoesNotThrow {
            listOf(
                BehovLøsningTestOppbevaringsboks(setOf("behovEn"), "en", UUID.randomUUID()),
                BehovLøsningTestOppbevaringsboks(setOf("behovTo"), "to", UUID.randomUUID()),
                BehovLøsningTestOppbevaringsboks(setOf("behovTre"), "tre", UUID.randomUUID())
            ).valider()
        }
    }

    @Test
    fun `oppbevaringsbokser med samme etikett`() {
        val feil = assertThrows<IllegalArgumentException> {
            listOf(
                BehovLøsningTestOppbevaringsboks(setOf("behovEn"), "en", UUID.randomUUID()),
                BehovLøsningTestOppbevaringsboks(setOf("behovTo"), "en", UUID.randomUUID()),
                BehovLøsningTestOppbevaringsboks(setOf("behovTre"), "tre", UUID.randomUUID())
            ).valider()
        }
        assertEquals("Det er flere oppbevaringsbokser med samme etiketter!!", feil.message)
    }

    @Test
    fun `oppbevaringsbokser med samme behov`() {
        val feil = assertThrows<IllegalArgumentException> {
            listOf(
                BehovLøsningTestOppbevaringsboks(setOf("behovEn"), "en", UUID.randomUUID()),
                BehovLøsningTestOppbevaringsboks(setOf("behovEn"), "to", UUID.randomUUID()),
                BehovLøsningTestOppbevaringsboks(setOf("behovTre"), "tre", UUID.randomUUID())
            ).valider()
        }
        assertEquals("Det er flere oppbevaringsbokser som håndterer samme behov!!", feil.message)
    }
}
