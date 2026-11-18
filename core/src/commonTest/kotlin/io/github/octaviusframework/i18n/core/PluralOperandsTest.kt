package io.github.octaviusframework.i18n.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PluralOperandsTest {

    /** The operands as `i v w f t`. */
    private fun operands(text: String): String = PluralOperands.of(text)!!.run { "$i $v $w $f $t" }

    @Test
    fun wholeNumbers() {
        assertEquals("0 0 0 0 0", operands("0"))
        assertEquals("22 0 0 0 0", operands("22"))
        assertEquals("5 0 0 0 0", operands("-5"))
    }

    @Test
    fun fractionDigitsAreCountedAsWritten() {
        assertEquals("1 1 1 5 5", operands("1.5"))
        assertEquals("1 2 1 50 5", operands("1.50"))
        assertEquals("1 1 0 0 0", operands("1.0"))
        assertEquals("1 2 2 5 5", operands("1.05"))
    }

    @Test
    fun anExponentMovesThePoint() {
        assertEquals("10000000000 0 0 0 0", operands("1.0E10"))
        assertEquals("1500 0 0 0 0", operands("1.5E3"))
        assertEquals("0 4 4 15 15", operands("1.5E-3"))
        assertEquals("1000 0 0 0 0", operands("1e+3"))
    }

    @Test
    fun aPartLongerThan18DigitsKeepsItsLast18AboveTenTo18() {
        assertEquals(1_000_000_000_000_000_002L, PluralOperands.of("1000000000000000000002")!!.i)
        assertEquals(1_000_000_000_000_000_000L, PluralOperands.of("1.0E30")!!.i)
    }

    @Test
    fun aCountWithNoDigitsHasNoOperands() {
        assertNull(PluralOperands.of(Double.NaN))
        assertNull(PluralOperands.of(Double.POSITIVE_INFINITY))
        assertNull(PluralOperands.of("."))
    }

    @Test
    fun wholeNumberTypesHaveNoFractionDigits() {
        for (count in listOf<Number>(3, 3L, 3.toShort(), 3.toByte())) {
            assertEquals(0L, PluralOperands.of(count)!!.v, "for ${count::class}")
        }
        assertEquals(1L, PluralOperands.of(1.5)!!.v)
        assertEquals(1L, PluralOperands.of(1.5f)!!.v)
    }
}
