package no.nav.helse.spiskammerset.opprydding_dev

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import org.intellij.lang.annotations.Language
import java.sql.Connection
import javax.sql.DataSource
import kotlin.use


internal class SlettPersonRiver(
    rapidsConnection: RapidsConnection,
    private val dataSource: DataSource
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition { it.requireValue("@event_name", "slett_person") }
            validate {
                it.requireKey("@id", "fødselsnummer")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val personidentifikator = packet["fødselsnummer"].asText()

        dataSource.connection {
            this.slettPerson(personidentifikator)
        }

        context.publish(personidentifikator, lagPersonSlettet(personidentifikator))
    }

    @Language("JSON")
    private fun lagPersonSlettet(personidentifikator: String): String {
        return """
            {
                "@event_name": "person_slettet",
                "fødselsnummer": "$personidentifikator"
            }
        """.trimIndent()
    }
}

private fun Connection.slettPerson(personidentifikator: String) {
    // Finn alle hyllenummer som tilhører denne personen
    @Language("PostgreSQL")
    val finnHyllerQuery = "SELECT hyllenummer FROM hylleeier WHERE personidentifikator = ?"
    val hyllenumre = mutableListOf<Long>()

    prepareStatement(finnHyllerQuery).use { statement ->
        statement.setString(1, personidentifikator)
        statement.executeQuery().use { resultSet ->
            while (resultSet.next()) {
                hyllenumre.add(resultSet.getLong("hyllenummer"))
            }
        }
    }

    if (hyllenumre.isEmpty()) return

    // Slett hyller - CASCADE DELETE sørger automatisk for at alt blir slettet:
    // - forsikring (CASCADE DELETE)
    // - hendelser_paa_hylla (CASCADE DELETE)
    // - hylleeier (CASCADE DELETE)
    val placeholders = hyllenumre.joinToString(",") { "?" }
    @Language("PostgreSQL")
    val slettHyllerQuery = "DELETE FROM hylle WHERE hyllenummer IN ($placeholders)"
    prepareStatement(slettHyllerQuery).use { statement ->
        hyllenumre.forEachIndexed { index, hyllenummer ->
            statement.setLong(index + 1, hyllenummer)
        }
        statement.executeUpdate()
    }
}
