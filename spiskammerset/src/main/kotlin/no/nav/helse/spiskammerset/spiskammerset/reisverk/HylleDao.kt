package no.nav.helse.spiskammerset.spiskammerset.reisverk

import java.sql.Connection
import no.nav.helse.spiskammerset.oppbevaringsboks.Hyllenummer

internal fun Connection.finnRettHylle(hendelse: Hendelse): Hyllenummer {
    return Hyllenummer(1L)
}

