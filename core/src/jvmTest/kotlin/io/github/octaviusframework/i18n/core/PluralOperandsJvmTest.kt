package io.github.octaviusframework.i18n.core

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class PluralOperandsJvmTest {

    @Test
    fun aDoubleWithNoFractionalPartStillShowsOneFractionDigit() {
        assertEquals("1.0", 1.0.toString())
        assertEquals(1L, PluralOperands.of(1.0)!!.v)
    }

    @Test
    fun aDoubleTheJvmPrintsWithAnExponentIsWrittenInFull() {
        assertEquals("1.0E10", 1.0E10.toString())
        assertEquals("10\u00A0000\u00A0000\u00A0000", NumberFormat(decimal = ",", group = "\u00A0", minimumGrouping = 2).format(1.0E10))
        assertEquals("1,0", NumberFormat(decimal = ",").format(1.0))
    }

    @Test
    fun aBigDecimalKeepsTheDigitsItWasWrittenWith() {
        assertEquals(2L, PluralOperands.of(BigDecimal("1.50"))!!.v)
        assertEquals(1_000L, PluralOperands.of(BigDecimal("1E+3"))!!.i)
    }
}
