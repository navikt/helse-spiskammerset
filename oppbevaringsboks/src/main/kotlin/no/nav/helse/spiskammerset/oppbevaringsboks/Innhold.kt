package no.nav.helse.spiskammerset.oppbevaringsboks

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

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
    fun tilJson() = mapper.writeValueAsString(innhold.plus("versjon" to versjon.nummer))

    private companion object {
        val mapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
}

sealed interface Innholdsstatus {
    data object EndretInnhold: Innholdsstatus
    data object UendretInnhold: Innholdsstatus
}
