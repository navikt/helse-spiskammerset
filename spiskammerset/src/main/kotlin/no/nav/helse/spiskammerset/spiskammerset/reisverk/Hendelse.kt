package no.nav.helse.spiskammerset.spiskammerset.reisverk

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
data class EndringId(val id: UUID) {
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

data class Hendelse(
    val hendelseId: HendelseId,
    val personidentifikator: Personidentifikator,
    val vedtaksperiodeId: VedtaksperiodeId,
    val behandlingId: BehandlingId,
    val endringId: EndringId,
    val periode: Periode,
    val yrkesaktivitetstype: Yrkesaktivitetstype,
    val organisasjonsnummer: Organisasjonsnummer?,
    val json: ObjectNode
) {
    companion object {
        fun opprett(json: ObjectNode): Hendelse {
            val yrkesaktivitetstype = Yrkesaktivitetstype(json["yrkesaktivitetstype"].asText())
            return Hendelse(
                hendelseId = HendelseId(UUID.fromString(json["@id"].asText())),
                personidentifikator = Personidentifikator(json["fødselsnummer"].asText()),
                vedtaksperiodeId = VedtaksperiodeId(UUID.fromString(json["vedtaksperiodeId"].asText())),
                behandlingId = BehandlingId(UUID.fromString(json["behandlingId"].asText())),
                endringId = EndringId(UUID.fromString(json["endringId"].asText())),
                periode = Periode(
                    fom = LocalDate.parse(json["fom"].asText()),
                    tom = LocalDate.parse(json["tom"].asText())
                ),
                yrkesaktivitetstype = yrkesaktivitetstype,
                organisasjonsnummer = when (yrkesaktivitetstype.erArbeidstaker) {
                    true -> Organisasjonsnummer(json["organisasjonsnummer"].asText())
                    false -> null
                },
                json = json
            )
        }
    }
}
