package io.github.octaviusframework.i18n

import io.github.octaviusframework.i18n.core.PluralOperands
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CldrPluralsTest {

    private fun cardinal(language: String) = PluralKind.CARDINAL.cldr.ruleFor(language)

    private fun ordinal(language: String) = PluralKind.ORDINAL.cldr.ruleFor(language)

    /** The form [rule] names for [operands], read off its conditions as the generated `when` reads them. */
    private fun form(rule: PluralRule, operands: PluralOperands): String =
        rule.conditions.entries.firstOrNull { it.value.matches(operands) }?.key ?: "other"

    private fun Condition.matches(o: PluralOperands): Boolean = when (this) {
        is Condition.AnyOf -> parts.any { it.matches(o) }
        is Condition.AllOf -> parts.all { it.matches(o) }
        is Condition.Relation -> {
            val operand = when (operand) {
                'n', 'i' -> o.i
                'v' -> o.v
                'w' -> o.w
                'f' -> o.f
                't' -> o.t
                else -> o.e
            }
            val value = modulus?.let { operand % it } ?: operand
            val inAny = ranges.any { value in it } && (this.operand != 'n' || o.t == 0L)
            inAny != negated
        }
    }

    /**
     * The numbers a sample list such as `0, 2~16, 100, …` or `0.0~1.5` stands for, as written. A range of decimals
     * steps by its last digit. Compact samples such as `1c6` are left out: a printed count has no exponent.
     */
    private fun samples(list: String): List<String> = list.split(',').map { it.trim() }
        .filter { it.isNotEmpty() && it != "…" && 'c' !in it && 'e' !in it }
        .flatMap { sample ->
            val (from, to) = sample.split('~').let { it.first() to it.last() }
            val start = BigDecimal(from)
            val end = BigDecimal(to)
            val step = BigDecimal.ONE.movePointLeft(start.scale())
            generateSequence(start) { it + step }.takeWhile { it <= end }.map { it.toPlainString() }.toList()
        }

    /** How many samples CLDR gives for [kind], each checked against the rule it is given for. */
    private fun checkSamples(kind: PluralKind): Int {
        var checked = 0
        for ((locale, entry) in kind.cldr.entries) {
            val rule = PluralRule.parse(locale, kind, entry)
            for ((form, text) in PluralRule.texts(entry)) {
                for (list in text.substringAfter('@', "").split('@').filter { it.isNotBlank() }) {
                    for (sample in samples(list.substringAfter(' '))) {
                        assertEquals(form, form(rule, PluralOperands.of(BigDecimal(sample))!!), "$kind $locale, $sample")
                        checked++
                    }
                }
            }
        }
        return checked
    }

    // Every locale has samples, so a parse that read none would show in the count
    @Test
    fun `every CLDR plural rule names the form of every sample CLDR gives for it`() {
        assertEquals(12_259, checkSamples(PluralKind.CARDINAL))
    }

    @Test
    fun `every CLDR ordinal rule names the form of every sample CLDR gives for it`() {
        assertEquals(2_732, checkSamples(PluralKind.ORDINAL))
    }

    @Test
    fun `a language code finds the rule of its language when CLDR has none for the code`() {
        assertEquals("pl", cardinal("pl")?.locale)
        assertEquals("pt", cardinal("pt-BR")?.locale)
        assertEquals("pt-PT", cardinal("pt-PT")?.locale)
        assertEquals("pt-PT", cardinal("PT_pt")?.locale)
        assertEquals("en", cardinal("en-GB")?.locale)
        assertEquals("kok-Latn", cardinal("kok-Latn-IN")?.locale)
        assertNull(cardinal("polish"))
    }

    @Test
    fun `ordinal rules are looked up the same way, and a locale CLDR has none for has no rule`() {
        assertEquals("en", ordinal("en-GB")?.locale)
        assertEquals("pt", ordinal("pt-PT")?.locale)
        assertEquals(setOf("other"), ordinal("pl")?.forms)
        assertNull(ordinal("eo"))
        assertEquals("few", form(ordinal("en")!!, PluralOperands.of(23)!!))
        assertEquals("other", form(ordinal("en")!!, PluralOperands.of(13)!!))
    }

    @Test
    fun `brazilian and european portuguese count zero differently`() {
        val zero = PluralOperands.of(0)!!
        assertEquals("one", form(cardinal("pt-BR")!!, zero))
        assertEquals("other", form(cardinal("pt-PT")!!, zero))
    }

    @Test
    fun `a rule becomes a when over the operands`() {
        assertEquals(
            """
            /** The plural form of [count] by the CLDR ${PluralKind.CARDINAL.cldr.version} rule for `lv`. */
            private fun ___form(count: kotlin.Number): kotlin.String {
                val o = io.github.octaviusframework.i18n.core.PluralOperands.of(count) ?: return "other"
                return when {
                    (o.t == 0L && o.i % 10L == 0L) || (o.t == 0L && o.i % 100L in 11L..19L) || o.v == 2L && o.f % 100L in 11L..19L -> "zero"
                    (o.t == 0L && o.i % 10L == 1L) && !(o.t == 0L && o.i % 100L == 11L) || o.v == 2L && o.f % 10L == 1L && o.f % 100L != 11L || o.v != 2L && o.f % 10L == 1L -> "one"
                    else -> "other"
                }
            }

            """.trimIndent(),
            cardinal("lv")!!.functionSource(),
        )
    }
}
