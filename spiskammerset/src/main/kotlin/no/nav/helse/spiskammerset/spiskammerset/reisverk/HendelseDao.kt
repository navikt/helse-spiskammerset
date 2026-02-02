package no.nav.helse.spiskammerset.spiskammerset.reisverk

import java.sql.Connection
import no.nav.helse.spiskammerset.oppbevaringsboks.Hyllenummer

internal fun Connection.håndterTidligere(hendelse: Hendelse): Boolean {
    return true
}

internal fun Connection.lagreHendelse(hendelse: Hendelse, endredeHyller: Set<Hyllenummer>) {
}
