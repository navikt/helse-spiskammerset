package no.nav.helse.spiskammerset

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import java.util.*
import kotlin.test.assertEquals
import kotliquery.Session
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TransaksjonTest {
    @BeforeEach
    fun reset() {
        DatabaseFixture.reset()
    }

    @Test
    fun `melding og grunnlagsdata lagres normalt ned`() {
        testMedRepositories(object : RepositoryFactory {
            override fun meldingRepository(session: Session) = MeldingRepositoryImpl(session)
            override fun grunnlagsdataRepository(session: Session) = GrunnlagsdataRepositoryImpl(session)
        })

        // Then:
        assertEquals(1, countMeldingRows())
        assertEquals(1, countGrunnlagsdataRows())
    }

    @Test
    fun `melding lagres ikke hvis lagring av grunnlagsdata feiler`() {
        assertThrows<IllegalStateException> {
            testMedRepositories(object : RepositoryFactory {
                override fun meldingRepository(session: Session) = MeldingRepositoryImpl(session)

                override fun grunnlagsdataRepository(session: Session) = object : GrunnlagsdataRepository {
                    override fun lagre(dto: GrunnlagsdataDto) {
                        error("Her feiler vi med vilje for å teste transaksjoner")
                    }
                }
            })
        }

        // Then:
        assertEquals(0, countMeldingRows())
        assertEquals(0, countGrunnlagsdataRows())
    }

    private fun testMedRepositories(repositoryFactory: RepositoryFactory) {
        // Given:
        val rapid = TestRapid().also {
            LøsningContentEnricherRiver(it, DatabaseFixture.dataSource, repositoryFactory)
        }

        val meldingId = UUID.randomUUID()
        val inputJson = """
                {
                    "@id": "$meldingId",
                    "@behov": ["SelvstendigForsikring"],
                    "@final": true,
                    "@event_name": "behov",
                    "@opprettet": "2024-06-01T12:00:00",
                    "fødselsnummer": "01020312345",
                    "@lagreLøsninger": true,
                    "SelvstendigForsikring": {
                        "skjæringstidspunkt": "2024-05-01"
                    },
                    "@løsning": {
                        "SelvstendigForsikring": {
                            "forsikringstype": "HundreProsentFraDagSytten",
                            "premiegrunnlag": 12345,
                            "startdato": "2024-01-01",
                            "sluttdato": "2024-12-31"
                        }
                    }
                }
            """.trimIndent()
        assertEquals(0, countMeldingRows())
        assertEquals(0, countGrunnlagsdataRows())

        // When:
        rapid.sendTestMessage(inputJson)
    }

    private fun countGrunnlagsdataRows(): Int = DatabaseFixture.countGrunnlagsdataRows()

    private fun countMeldingRows(): Int = DatabaseFixture.countMeldingRows()
}
