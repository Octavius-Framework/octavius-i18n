package io.github.octaviusframework.i18n.core

import kotlin.test.Test
import kotlin.test.assertEquals

class PluralOperandsJsTest {

    @Test
    fun aDoubleWithNoFractionalPartPrintsAndCountsAsAWholeNumber() {
        assertEquals("1", 1.0.toString())
        assertEquals(0L, PluralOperands.of(1.0)!!.v)
        assertEquals(1L, PluralOperands.of(1.5)!!.v)
    }
}
