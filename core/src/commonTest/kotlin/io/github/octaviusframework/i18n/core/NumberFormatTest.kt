package io.github.octaviusframework.i18n.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NumberFormatTest {

    private val polish = NumberFormat(decimal = ",", group = "\u00A0", minimumGrouping = 2)
    private val english = NumberFormat()
    private val hindi = NumberFormat(primaryGrouping = 3, secondaryGrouping = 2)

    @Test
    fun separatorsAreTheFormats() {
        assertEquals("1,5", polish.format(1.5))
        assertEquals("1.5", english.format(1.5))
        assertEquals("10'000", NumberFormat(group = "'").format(10000))
    }

    @Test
    fun theMinimumGroupingKeepsShortNumbersWhole() {
        assertEquals("1000", polish.format(1000))
        assertEquals("10\u00A0000", polish.format(10000))
        assertEquals("1,000", english.format(1000))
    }

    @Test
    fun groupsBeforeTheFirstHaveTheSecondarySize() {
        assertEquals("12,34,567", hindi.format(1234567))
        assertEquals("1,234,567", english.format(1234567))
    }

    @Test
    fun noGroupingWhenTheGroupIsZero() {
        assertEquals("1234567", NumberFormat(primaryGrouping = 0).format(1234567))
    }

    @Test
    fun theDigitsAreTheFormats() {
        val eastern = NumberFormat("\u066B", "\u066C", "\u061C-", 1, 3, 3, (0..9).map { (0x0660 + it).toChar().toString() })
        assertEquals("\u0661\u0662\u066B\u0665", eastern.format(12.5))
        assertEquals("\u061C-\u0665", eastern.format(-5))
        // Adlam digits lie outside the Basic Multilingual Plane, two UTF-16 units each. Each digit is built from its
        // two units as chars: Kotlin/JS's incremental cache keeps a string literal as UTF-8, so a lone surrogate in
        // one comes back as `?`
        val adlam = NumberFormat(digits = (0..9).map { charArrayOf('\uD83A', (0xDD50 + it).toChar()).concatToString() })
        assertEquals("\uD83A\uDD51\uD83A\uDD50", adlam.format(10))
    }

    @Test
    fun aNegativeNumberTakesTheFormatsMinus() {
        assertEquals("-5", polish.format(-5))
        assertEquals("\u22125", NumberFormat(minus = "\u2212").format(-5))
    }

    @Test
    fun theDigitsWrittenAreTheOnesKotlinPrints() {
        assertEquals("0,0015", polish.format(1.5E-3))
        assertEquals("3\u00A0000\u00A0000\u00A0002", polish.format(3_000_000_002L))
    }

    @Test
    fun aCountWithNoDigitsIsWrittenAsItPrints() {
        assertEquals("NaN", polish.format(Double.NaN))
    }

    @Test
    fun aFormatHasTenDigits() {
        assertFailsWith<IllegalArgumentException> { NumberFormat(digits = listOf("0", "1")) }
    }
}
