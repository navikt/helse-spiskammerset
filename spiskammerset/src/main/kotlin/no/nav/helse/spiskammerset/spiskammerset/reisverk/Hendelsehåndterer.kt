package no.nav.helse.spiskammerset.spiskammerset.reisverk

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.transaction
import no.nav.helse.spiskammerset.oppbevaringsboks.Hyllenummer
import no.nav.helse.spiskammerset.oppbevaringsboks.Innholdsstatus
import no.nav.helse.spiskammerset.oppbevaringsboks.Oppbevaringsboks
import javax.sql.DataSource

internal class Hendelsehåndterer(private val dataSource: DataSource, private val oppbevaringsbokser: List<Oppbevaringsboks>) {
    fun håndter(json: ObjectNode) {
        val hendelse = Hendelse.opprett(json)

        dataSource.connection {
            transaction {
                if (håndtertTidligere(hendelse)) return@transaction

                val endredeHyller = mutableSetOf<Hyllenummer>()

                hendelse.behandlinger.forEach { behandling ->
                    val hyllenummer = finnEllerOpprettHylle(behandling).also { hyllestatus ->
                        when (hyllestatus) {
                            is Hyllestatus.UendretHylle -> {}
                            is Hyllestatus.NyHylle,
                            is Hyllestatus.EndretHylle -> endredeHyller.add(hyllestatus.hyllenummer)
                        }
                    }.hyllenummer

                    oppbevaringsbokser.forEach { oppbevaringsboks ->
                        when (oppbevaringsboks.leggPå(hyllenummer, json, this)) {
                            Innholdsstatus.UendretInnhold -> {}
                            Innholdsstatus.EndretInnhold -> endredeHyller.add(hyllenummer)
                        }
                    }
                }
                if (endredeHyller.isNotEmpty()) {
                    lagreHendelse(hendelse, endredeHyller)
                }
            }
        }
    }
}
