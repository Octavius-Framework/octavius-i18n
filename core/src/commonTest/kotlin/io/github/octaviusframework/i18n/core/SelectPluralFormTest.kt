package io.github.octaviusframework.i18n.core

import kotlin.test.Test
import kotlin.test.assertEquals

class SelectPluralFormTest {

    private fun form(lang: String, count: Number) = OctaviusI18n.selectPluralForm(lang, count)

    @Test
    fun wholeNumbersOfEveryTypeAreCountedAlike() {
        for (count in listOf<Number>(3, 3L, 3.toShort(), 3.toByte())) {
            assertEquals("few", form("pl", count), "for ${count::class}")
        }
        assertEquals("many", form("pl", 5L))
        assertEquals("one", form("en", 1L))
    }

    @Test
    fun fractionsAreCountedAsFractions() {
        assertEquals("other", form("pl", 1.5))
        assertEquals("other", form("pl", 1.5f))
        assertEquals("other", form("pl", 2.5))
    }

    @Test
    fun aLongBeyondIntIsCountedByItsLastDigits() {
        assertEquals("few", form("pl", 3_000_000_002L))
        assertEquals("many", form("pl", 3_000_000_005L))
        assertEquals("many", form("pl", 3_000_000_012L))
        assertEquals("other", form("en", 4_000_000_001L))
    }

    @Test
    fun anIntIsAWholeNumberOnEveryPlatform() {
        assertEquals("one", form("pl", 1))
        assertEquals("few", form("pl", 22))
    }
}
