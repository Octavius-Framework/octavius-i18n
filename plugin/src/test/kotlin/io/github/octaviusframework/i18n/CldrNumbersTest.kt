package io.github.octaviusframework.i18n

import kotlin.test.Test
import kotlin.test.assertEquals

class CldrNumbersTest {

    @Test
    fun `the plural rules, the ordinal rules and the number formats are of one CLDR release`() {
        assertEquals(PluralKind.CARDINAL.cldr.version, PluralKind.ORDINAL.cldr.version)
        assertEquals(PluralKind.CARDINAL.cldr.version, CldrNumbers.version)
    }

    @Test
    fun `a language code finds the format of its language when CLDR has none for the code`() {
        assertEquals(CldrNumbers.formatFor("pt"), CldrNumbers.formatFor("pt-BR"))
        assertEquals(2, CldrNumbers.formatFor("PT_pt").minimumGrouping)
        assertEquals(1, CldrNumbers.formatFor("pt").minimumGrouping)
        assertEquals(CldrNumbers.formatFor("und"), CldrNumbers.formatFor("polish"))
    }

    @Test
    fun `a format is the one CLDR gives the locale`() {
        val polish = CldrNumbers.formatFor("pl")
        assertEquals(CldrNumberFormat(",", "\u00A0", "-", 2, 3, 3, (0..9).map { it.toString() }), polish)
        assertEquals(2, CldrNumbers.formatFor("hi").secondaryGrouping)
        assertEquals("\u0660", CldrNumbers.formatFor("ar-EG").digits.first())
        assertEquals("0", CldrNumbers.formatFor("ar").digits.first())
    }

    @Test
    fun `a format becomes a NumberFormat with every character outside ASCII escaped`() {
        assertEquals(
            "io.github.octaviusframework.i18n.core.NumberFormat(\",\", \"\\u00A0\", \"-\", 2, 3, 3, " +
                "io.github.octaviusframework.i18n.core.NumberFormat.LATIN_DIGITS)",
            CldrNumbers.formatFor("pl").toKotlin(),
        )
        assertEquals(
            "kotlin.collections.listOf(\"\\uD83A\\uDD50\"",
            CldrNumbers.formatFor("ff-Adlm").toKotlin().substringAfter("3, 3, ").substringBefore(','),
        )
    }
}
