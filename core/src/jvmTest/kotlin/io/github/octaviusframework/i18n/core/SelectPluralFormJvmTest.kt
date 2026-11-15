package io.github.octaviusframework.i18n.core

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class SelectPluralFormJvmTest {

    @Test
    fun aDoubleWithNoFractionalPartIsStillAFraction() {
        assertEquals("other", OctaviusI18n.selectPluralForm("pl", 1.0 as Number))
    }

    @Test
    fun anotherNumberTypeIsWholeWhenItHasNoFractionalPart() {
        assertEquals("few", OctaviusI18n.selectPluralForm("pl", BigDecimal("3")))
        assertEquals("other", OctaviusI18n.selectPluralForm("pl", BigDecimal("1.5")))
    }
}
