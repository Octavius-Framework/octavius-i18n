package io.github.octaviusframework.i18n.core

import kotlin.math.abs

/**
 * Main utility object for Octavius I18n plugin.
 * Handles current language state and plural rules.
 */
object OctaviusI18n {

    /**
     * The currently selected language code (e.g. "en", "pl").
     * Change this property to switch the application's locale at runtime.
     */
    var currentLanguage: String = "en"

    /**
     * Map of available plural rules by language code.
     * Contains built-in rules for English and Polish by default.
     */
    val pluralRules: MutableMap<String, PluralRule> = mutableMapOf(
        "en" to object : PluralRule {
            override fun selectForm(count: Int) = if (count == 1) "one" else "other"
            override fun selectForm(count: Double) = "other"
        },
        "pl" to object : PluralRule {
            override fun selectForm(count: Int) = when {
                count == 1 -> "one"
                count % 10 in 2..4 && count % 100 !in 12..14 -> "few"
                else -> "many"
            }
            override fun selectForm(count: Double) = "other"
        }
    )

    /**
     * Resolves the appropriate plural form for a given language and integer count.
     *
     * @param lang The language code (e.g. "en", "pl").
     * @param count The integer amount for which the plural form is evaluated.
     * @return The plural category string (e.g., "zero", "one", "two", "few", "many", "other").
     */
    fun selectPluralForm(lang: String, count: Int): String {
        val rule = pluralRules[lang] ?: pluralRules["en"] ?: object : PluralRule {
            override fun selectForm(count: Int) = "other"
            override fun selectForm(count: Double) = "other"
        }
        return rule.selectForm(count)
    }

    /**
     * Resolves the appropriate plural form for a given language and fractional count.
     *
     * @param lang The language code (e.g. "en", "pl").
     * @param count The fractional amount for which the plural form is evaluated.
     * @return The plural category string (e.g., "zero", "one", "two", "few", "many", "other").
     */
    fun selectPluralForm(lang: String, count: Double): String {
        val rule = pluralRules[lang] ?: pluralRules["en"] ?: object : PluralRule {
            override fun selectForm(count: Int) = "other"
            override fun selectForm(count: Double) = "other"
        }
        return rule.selectForm(count)
    }

    /**
     * Resolves the plural form for [count] of any number type. Whole number types are counted as an `Int`, a
     * `Long` beyond the `Int` range by its last nine digits, and `Double` and `Float` as fractions. Kotlin/JS
     * cannot tell `1.0` from `1`, so there a `Double` with no fractional part is a whole number.
     */
    fun selectPluralForm(lang: String, count: Number): String {
        if (count is Long) return selectPluralForm(lang, countable(count))
        val hasFraction = count.toDouble() != count.toLong().toDouble()
        val fraction = if (untypedNumbers) hasFraction else count is Double || count is Float || hasFraction
        return if (fraction) selectPluralForm(lang, count.toDouble()) else selectPluralForm(lang, countable(count.toLong()))
    }

    /** True on Kotlin/JS, where every number passes both `is Int` and `is Double`. */
    private val untypedNumbers: Boolean = isInt(1.5)

    private fun isInt(number: Number) = number is Int

    /** [whole] as an `Int` with the same last nine digits, kept above 10^9 when it does not fit. */
    private fun countable(whole: Long): Int =
        if (whole in Int.MIN_VALUE..Int.MAX_VALUE) whole.toInt() else (1_000_000_000 + abs(whole % 1_000_000_000)).toInt()
}
