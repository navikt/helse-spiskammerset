package no.nav.helse.spiskammerset.spiskammerset.reisverk

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
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

    data class KomplettBehandling(
        val vedtaksperiodeId: VedtaksperiodeId,
        override val behandlingId: BehandlingId,
        val periode: Periode,
        val yrkesaktivitetstype: Yrkesaktivitetstype,
        val organisasjonsnummer: Organisasjonsnummer?,
    ): Behandling

    data class MinimalBehandling(
        override val behandlingId: BehandlingId,
        val periode: Periode?
    ): Behandling
}

data class Hendelse(
    val hendelseId: HendelseId,
    val personidentifikator: Personidentifikator,
    val behandlinger: List<Behandling>,
    val json: ObjectNode
) {
    companion object {
        fun opprett(json: ObjectNode): Hendelse {
            return Hendelse(
                hendelseId = HendelseId(UUID.fromString(json["@id"].asText())),
                personidentifikator = Personidentifikator(json["fødselsnummer"].asText()),
                json = json,
                behandlinger = when (json.hasNonNull("behandlinger")) {
                    true -> json.path("behandlinger").map { behandling(it) }
                    false -> listOf(behandling(json))
                }
            )
        }

        private fun behandling(json: JsonNode): Behandling {
            val yrkesaktivitetstype = Yrkesaktivitetstype(json["yrkesaktivitetstype"].asText())
            return Behandling.KomplettBehandling(
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
