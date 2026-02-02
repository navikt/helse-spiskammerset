package no.nav.helse.spiskammerset.spiskammerset.reisverk

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.transaction
import javax.sql.DataSource
import no.nav.helse.spiskammerset.oppbevaringsboks.Oppbevaringsboks

internal class Hendelsehåndterer(private val dataSource: DataSource, private val oppbevaringsbokser: List<Oppbevaringsboks>) {
    fun håndter(json: ObjectNode) {
        val hendelse = Hendelse.opprett(json)

        dataSource.connection {
            transaction {
                if (!lagreHendelse(hendelse)) return@transaction// Håndtert før

                hendelse.behandlinger.forEach { behandling ->
                    val hyllenummer = finnRettHylle(hendelse.hendelseId, hendelse.personidentifikator, behandling).hyllenummer

                    oppbevaringsbokser.forEach { oppbevaringsboks ->
                        oppbevaringsboks.leggPå(hyllenummer, json, this)
                    }
                }
            }
        }
    }
}
