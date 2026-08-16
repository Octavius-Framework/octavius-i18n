package io.github.octaviusframework.i18n.core

/**
 * Strategy for selecting the correct plural category (e.g. "one", "few", "many", "other")
 * for a given count, according to a specific language's pluralization rules.
 */
interface PluralRule {
    /**
     * Selects the plural category for an integer count.
     *
     * @param count The amount to evaluate.
     * @return The plural category string (e.g. "zero", "one", "two", "few", "many", "other").
     */
    fun selectForm(count: Int): String

    /**
     * Selects the plural category for a fractional count.
     *
     * @param count The amount to evaluate.
     * @return The plural category string (e.g. "zero", "one", "two", "few", "many", "other").
     */
    fun selectForm(count: Double): String
}
