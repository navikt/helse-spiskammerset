package no.nav.helse.spiskammerset.spiskammerset

import java.time.LocalDate
import java.time.Month

private fun Int.måned(måned: Month, år: Int) = LocalDate.of(år, måned, this)
internal fun Int.januar(år: Int) = måned(Month.JANUARY, år)
internal fun Int.februar(år: Int) = måned(Month.FEBRUARY, år)
internal fun Int.mars(år: Int) = måned(Month.MARCH, år)
internal fun Int.desember(år: Int) = måned(Month.DECEMBER, år)
internal val Int.januar get() = januar(2018)
internal val Int.februar get() = februar(2018)
internal val Int.mars get() = mars(2018)
