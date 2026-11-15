package io.github.octaviusframework.i18n.core

import kotlin.test.Test
import kotlin.test.assertEquals

class SelectPluralFormJsTest {

    @Test
    fun aDoubleWithNoFractionalPartIsAWholeNumber() {
        assertEquals("1", 1.0.toString())
        assertEquals("one", OctaviusI18n.selectPluralForm("pl", 1.0 as Number))
        assertEquals("other", OctaviusI18n.selectPluralForm("pl", 1.5 as Number))
    }
}
