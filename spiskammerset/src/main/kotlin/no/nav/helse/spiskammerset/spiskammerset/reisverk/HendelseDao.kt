package no.nav.helse.spiskammerset.spiskammerset.reisverk

import java.sql.Connection

internal fun Connection.lagreHendelse(hendelse: Hendelse): Boolean {
    return true
}
