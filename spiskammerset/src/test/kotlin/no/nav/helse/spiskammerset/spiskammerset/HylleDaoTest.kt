package no.nav.helse.spiskammerset.spiskammerset

import no.nav.helse.spiskammerset.spiskammerset.reisverk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import kotlin.random.Random

class HylleDaoTest {

    @Test
    fun `To hendelser for samme behandling gir samme hyllenummer`() = databaseTest { dataSource ->
        val behandlingFraHendelse1 = lagKomplettBehandling()
        val behandlingFraHendelse2 = behandlingFraHendelse1.copy(
            periode = Periode(
                fom = LocalDate.parse("2018-01-02"),
                tom = LocalDate.parse("2018-01-30")
            )
        )

        dataSource.connection.use { connection ->
            val hyllenummer1 = connection.finnEllerOpprettHylle(
                behandling = behandlingFraHendelse1
            ).hyllenummer

            val hyllenummer2 = connection.finnEllerOpprettHylle(
                behandling = behandlingFraHendelse2
            ).hyllenummer

            assertEquals(hyllenummer1, hyllenummer2)
        }
    }

    @Test
    fun `Finne hyller på person + periode`() = databaseTest { dataSource ->
        val personidentifikator = enUnikPersonidentifikator()
        val opprettetBehandling = lagKomplettBehandling(fom = 2.januar, tom = 30.januar, personidentifikator = personidentifikator)

        dataSource.connection.use { connection ->
            connection.finnEllerOpprettHylle(
                behandling = opprettetBehandling
            )

            val forventetHylle = opprettetBehandling.somHylle()
            assertEquals(emptyList<Hylle>(), connection.finnHyller(Periode(1.januar, 1.januar), personidentifikator)) // før
            assertEquals(forventetHylle, connection.finnHyller(Periode(2.januar, 2.januar), personidentifikator).single()) // snute
            assertEquals(forventetHylle, connection.finnHyller(Periode(15.januar, 15.januar), personidentifikator).single()) // mage
            assertEquals(forventetHylle, connection.finnHyller(Periode(30.januar, 30.januar), personidentifikator).single()) // hale
            assertEquals(emptyList<Hylle>(), connection.finnHyller(Periode(31.januar, 31.januar), personidentifikator)) // etter
        }
    }

    @Test
    fun `En hyllereise`() = databaseTest { dataSource ->
        val behandlingFraHendelse1 = lagKomplettBehandling()
        val behandlingFraHendelse2 = behandlingFraHendelse1.copy(
            periode = Periode(2.januar, 30.januar)
        )
        val behandlingFrahendelse3 = behandlingFraHendelse2.copy()

        dataSource.connection.use { connection ->
            val hyllestatus1 = connection.finnEllerOpprettHylle(
                behandling = behandlingFraHendelse1
            )
            val hyllenummer = hyllestatus1.hyllenummer
            assertEquals(Hyllestatus.NyHylle::class, hyllestatus1::class)

            assertEquals(Hyllestatus.EndretHylle(hyllenummer), connection.finnEllerOpprettHylle(behandlingFraHendelse2))
            assertEquals(Hyllestatus.UendretHylle(hyllenummer), connection.finnEllerOpprettHylle(behandlingFrahendelse3))

            assertEquals(Hyllestatus.EndretHylle(hyllenummer), connection.finnEllerOpprettHylle(
                behandling = lagMinimalBehandling(
                    personidentifikator = behandlingFraHendelse1.personidentifikator,
                    behandlingId = behandlingFraHendelse1.behandlingId,
                    periode = Periode(3.januar, 30.januar)
                )
            ))

            assertEquals(Hyllestatus.UendretHylle(hyllenummer), connection.finnEllerOpprettHylle(
                behandling = lagMinimalBehandling(
                    personidentifikator = behandlingFraHendelse1.personidentifikator,
                    behandlingId = behandlingFraHendelse1.behandlingId
                )
            ))

            assertEquals(Hyllestatus.UendretHylle(hyllenummer), connection.finnEllerOpprettHylle(
                behandling = lagMinimalBehandling(
                    personidentifikator = behandlingFraHendelse1.personidentifikator,
                    behandlingId = behandlingFraHendelse1.behandlingId,
                    periode = Periode(3.januar, 30.januar)
                )
            ))
        }
    }

    @Test
    fun `en behandling med fler personidentifikatorer`() = databaseTest { dataSource ->
        val personId1 = enUnikPersonidentifikator()
        val personId2 = enUnikPersonidentifikator()
        val personId3 = enUnikPersonidentifikator()

        val behandlingPersonId1 = lagKomplettBehandling(
            personidentifikator = personId1,
            fom = 17.januar,
            tom = 31.januar
        )
        val behandlingPersonId2 = lagMinimalBehandling(
            behandlingId = behandlingPersonId1.behandlingId,
            personidentifikator = personId2
        )

        val behandlingPersonId3 = lagMinimalBehandling(
            behandlingId = behandlingPersonId1.behandlingId,
            personidentifikator = personId3,
            periode = Periode(1.januar, 31.januar)
        )

        dataSource.connection.use { connection ->
            val førsteHyllestatus = connection.finnEllerOpprettHylle(
                behandling = behandlingPersonId1
            )
            val hyllenummer = førsteHyllestatus.hyllenummer
            assertEquals(Hyllestatus.NyHylle::class, førsteHyllestatus::class)
            assertEquals(Hyllestatus.EndretHylle(hyllenummer), connection.finnEllerOpprettHylle(behandlingPersonId2))
            assertEquals(Hyllestatus.EndretHylle(hyllenummer), connection.finnEllerOpprettHylle(behandlingPersonId3))


            val forventet = Hylle(
                behandlingId = behandlingPersonId1.behandlingId,
                vedtaksperiodeId = behandlingPersonId1.vedtaksperiodeId,
                periode = Periode(1.januar, 31.januar),
                yrkesaktivitetstype = Yrkesaktivitetstype("ARBEIDSTAKER"),
                organisasjonsnummer = Organisasjonsnummer("999999999")
            )
            val midtI = Periode(15.januar, 15.januar)

            assertEquals(listOf(forventet), connection.finnHyller(midtI, personId1))
            assertEquals(listOf(forventet), connection.finnHyller(midtI, personId2))
            assertEquals(listOf(forventet), connection.finnHyller(midtI, personId3))
            assertEquals(listOf(forventet), connection.finnHyller(midtI, personId3, personId1, personId2))
        }
    }

    @Test
    fun `en vedtaksperiode som strekkes`() = databaseTest { dataSource ->
        val personidentifikator = enUnikPersonidentifikator()
        val vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID())
        val førsteBehandling = lagKomplettBehandling(17.januar, 25.januar, vedtaksperiodeId = vedtaksperiodeId, personidentifikator = personidentifikator)
        val strekkesISnuten = lagKomplettBehandling(1.januar, 25.januar, vedtaksperiodeId = vedtaksperiodeId, personidentifikator = personidentifikator)
        val strekkesIHalen = lagKomplettBehandling(1.januar, 31.januar, vedtaksperiodeId = vedtaksperiodeId, personidentifikator = personidentifikator)

        dataSource.connection.use { connection ->
            connection.finnEllerOpprettHylle(førsteBehandling)
            connection.finnEllerOpprettHylle(strekkesISnuten)
            connection.finnEllerOpprettHylle(strekkesIHalen)
            val forventet = setOf(førsteBehandling.behandlingId, strekkesISnuten.behandlingId, strekkesIHalen.behandlingId)

            val spørPåSnute = connection.finnHyller(Periode(1.januar, 1.januar), personidentifikator).map { it.behandlingId }.toSet()
            val spørPåMage = connection.finnHyller(Periode(15.januar, 15.januar), personidentifikator).map { it.behandlingId }.toSet()
            val spørPåSHale = connection.finnHyller(Periode(31.januar, 31.januar), personidentifikator).map { it.behandlingId }.toSet()

            assertEquals(forventet, spørPåSnute)
            assertEquals(forventet, spørPåMage)
            assertEquals(forventet, spørPåSHale)
        }
    }

    private fun lagKomplettBehandling(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        personidentifikator: Personidentifikator = enUnikPersonidentifikator(),
        vedtaksperiodeId: VedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID())
    ) = Behandling.KomplettBehandling(
        vedtaksperiodeId = vedtaksperiodeId,
        behandlingId = BehandlingId(UUID.randomUUID()),
        periode = Periode(fom, tom),
        yrkesaktivitetstype = Yrkesaktivitetstype("ARBEIDSTAKER"),
        organisasjonsnummer = Organisasjonsnummer("999999999"),
        personidentifikator = personidentifikator,
        opprettet = OffsetDateTime.MIN
    )

    private fun lagMinimalBehandling(behandlingId: BehandlingId, periode: Periode? = null, personidentifikator: Personidentifikator = enUnikPersonidentifikator()) = Behandling.MinimalBehandling(
        behandlingId = behandlingId,
        periode = periode,
        personidentifikator = personidentifikator
    )

    private companion object {
        fun Behandling.KomplettBehandling.somHylle(endretPeriode: Periode? = null) = Hylle(
            behandlingId = this.behandlingId,
            vedtaksperiodeId = this.vedtaksperiodeId,
            periode = endretPeriode ?: this.periode,
            yrkesaktivitetstype = yrkesaktivitetstype,
            organisasjonsnummer = organisasjonsnummer,
            opprettet = opprettet
        )
        private val min = 11_111_111_111L
        private val max = 99_999_999_999L
        fun enUnikPersonidentifikator() = Personidentifikator(Random.nextLong(min, max + 1).toString())
    }
}
