package io.github.octaviusframework.i18n

import io.github.octaviusframework.i18n.core.PluralOperands
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CldrPluralsTest {

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

    @Test
    fun `every CLDR rule names the form of every sample CLDR gives for it`() {
        var checked = 0
        for ((locale, entry) in CldrPlurals.entries) {
            val rule = PluralRule.parse(locale, entry)
            for ((form, text) in PluralRule.texts(entry)) {
                for (list in text.substringAfter('@', "").split('@').filter { it.isNotBlank() }) {
                    for (sample in samples(list.substringAfter(' '))) {
                        assertEquals(form, form(rule, PluralOperands.of(sample)!!), "$locale, $sample")
                        checked++
                    }
                }
            }
        }
        // Every locale has samples, so a parse that read none would show here
        assert(checked > 10_000) { "only $checked samples checked" }
    }

    @Test
    fun `a language code finds the rule of its language when CLDR has none for the code`() {
        assertEquals("pl", CldrPlurals.ruleFor("pl")?.locale)
        assertEquals("pt", CldrPlurals.ruleFor("pt-BR")?.locale)
        assertEquals("pt-PT", CldrPlurals.ruleFor("pt-PT")?.locale)
        assertEquals("pt-PT", CldrPlurals.ruleFor("PT_pt")?.locale)
        assertEquals("en", CldrPlurals.ruleFor("en-GB")?.locale)
        assertEquals("kok-Latn", CldrPlurals.ruleFor("kok-Latn-IN")?.locale)
        assertNull(CldrPlurals.ruleFor("polish"))
    }

    @Test
    fun `brazilian and european portuguese count zero differently`() {
        val zero = PluralOperands.of("0")!!
        assertEquals("one", form(CldrPlurals.ruleFor("pt-BR")!!, zero))
        assertEquals("other", form(CldrPlurals.ruleFor("pt-PT")!!, zero))
    }

    @Test
    fun `a rule becomes a when over the operands`() {
        assertEquals(
            """
            /** The plural form of [count] by the CLDR ${CldrPlurals.version} rule for `lv`. */
            private fun ___form(count: kotlin.Number): kotlin.String {
                val o = io.github.octaviusframework.i18n.core.PluralOperands.of(count) ?: return "other"
                return when {
                    (o.t == 0L && o.i % 10L == 0L) || (o.t == 0L && o.i % 100L in 11L..19L) || o.v == 2L && o.f % 100L in 11L..19L -> "zero"
                    (o.t == 0L && o.i % 10L == 1L) && !(o.t == 0L && o.i % 100L == 11L) || o.v == 2L && o.f % 10L == 1L && o.f % 100L != 11L || o.v != 2L && o.f % 10L == 1L -> "one"
                    else -> "other"
                }
            }

            """.trimIndent(),
            CldrPlurals.ruleFor("lv")!!.functionSource(),
        )
    }
}
