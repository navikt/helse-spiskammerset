package no.nav.helse.spiskammerset.oppbevaringsboks

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.collections.plus

data class Hyllenummer(val nummer: Long)

data class Versjon(val nummer: Int) {
    init {
        check(nummer > 0)
    }
}

data class Innhold(
    val versjon: Versjon,
    val innhold: Map<String, Any?>
) {
    private val medVersjon = innhold.plus("versjon" to versjon.nummer)

    fun tilJson() = mapper.writeValueAsString(medVersjon)

    companion object {
        fun Map<String, Innhold?>.tilJson() = mapper.writeValueAsString(mapValues { it.value?.medVersjon })

        private val mapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
}

sealed interface Innholdsstatus {
    data object EndretInnhold: Innholdsstatus
    data object UendretInnhold: Innholdsstatus
}
