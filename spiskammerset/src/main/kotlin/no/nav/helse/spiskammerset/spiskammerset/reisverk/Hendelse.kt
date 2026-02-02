package no.nav.helse.spiskammerset.spiskammerset.reisverk

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class Personidentifikator(val id: String) {
    init { check(id.matches("\\d+".toRegex())) }
    override fun toString() = id
}
data class VedtaksperiodeId(val id: UUID) {
    override fun toString() = id.toString()
}
data class BehandlingId(val id: UUID) {
    override fun toString() = id.toString()
}
data class HendelseId(val id: UUID) {
    override fun toString() = id.toString()
}
data class Periode(val fom: LocalDate, val tom: LocalDate) {
    init { check(tom >= fom) { "Ugyldig periode $fom - $tom" } }
    override fun toString() = "$fom til $tom"
}
data class Yrkesaktivitetstype(val type: String) {
    init { check(type.isNotBlank()) { "Ugyldig yrkesaktivitetstype $type"} }
    val erArbeidstaker = type == "ARBEIDSTAKER"
    override fun toString() = type
}
data class Organisasjonsnummer(val organisasjonsnummer: String) {
    init { check(organisasjonsnummer.matches("\\d{9}".toRegex())) { "Ugyldig organisasjonsnummer $organisasjonsnummer"} }
    override fun toString() = organisasjonsnummer
}

sealed interface Behandling {
    val behandlingId: BehandlingId

    // Inneholder all informasjon som må til for å lage en "Hylle"
    data class KomplettBehandling(
        val personidentifikator: Personidentifikator,
        val vedtaksperiodeId: VedtaksperiodeId,
        override val behandlingId: BehandlingId,
        val periode: Periode,
        val yrkesaktivitetstype: Yrkesaktivitetstype,
        val organisasjonsnummer: Organisasjonsnummer?,
        val opprettet: OffsetDateTime = OffsetDateTime.MIN,
    ): Behandling {
    }

    // Inneholder kun referanse til behandlingen og det som kan endres på en behandling (per nå kun periode)
    data class MinimalBehandling(
        override val behandlingId: BehandlingId,
        val periode: Periode?
    ): Behandling
}

data class Hendelse(
    val hendelseId: HendelseId,
    val behandlinger: List<Behandling>,
    val json: ObjectNode
) {
    companion object {
        fun opprett(json: ObjectNode): Hendelse {
            return Hendelse(
                hendelseId = HendelseId(UUID.fromString(json["@id"].asText())),
                json = json,
                behandlinger = when (json.hasNonNull("behandlinger")) {
                    true -> json.path("behandlinger").map { behandling(it) }
                    false -> listOf(behandling(json))
                }
            )
        }

        private fun behandling(json: JsonNode): Behandling {
            // TODO: Mangler mapping om det bare er en "MinimalBehandling"
            val yrkesaktivitetstype = Yrkesaktivitetstype(json["yrkesaktivitetstype"].asText())
            return Behandling.KomplettBehandling(
                personidentifikator = Personidentifikator(json["fødselsnummer"].asText()),
                vedtaksperiodeId = VedtaksperiodeId(UUID.fromString(json["vedtaksperiodeId"].asText())),
                behandlingId = BehandlingId(UUID.fromString(json["behandlingId"].asText())),
                periode = Periode(
                    fom = LocalDate.parse(json["fom"].asText()),
                    tom = LocalDate.parse(json["tom"].asText())
                ),
                yrkesaktivitetstype = yrkesaktivitetstype,
                organisasjonsnummer = when (yrkesaktivitetstype.erArbeidstaker) {
                    true -> Organisasjonsnummer(json["organisasjonsnummer"].asText())
                    false -> null
                }
            )
        }
    }
}
