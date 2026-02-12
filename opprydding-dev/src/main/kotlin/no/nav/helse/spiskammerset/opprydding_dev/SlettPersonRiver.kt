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
    val query = "DELETE FROM hylleeier WHERE personidentifikator = ?"
    prepareStatement(query).use { statement ->
        statement.setString(1, personidentifikator)
        statement.executeUpdate()
    }
}
