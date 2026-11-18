package io.github.octaviusframework.i18n

/**
 * One language's CLDR plural rule of a [kind]: the [conditions] of its forms, in CLDR order. `other` has none and
 * takes every count the others leave.
 */
internal class PluralRule(val locale: String, val kind: PluralKind, val conditions: Map<String, Condition>) {

    /** The forms the rule names, `other` among them. */
    val forms: Set<String> = conditions.keys + "other"

    /**
     * The source of the private function named by the [kind] that names the form of a `count: Number`. A rule that
     * names only `other` needs none.
     */
    fun functionSource(): String {
        require(conditions.isNotEmpty()) { "$locale names only other" }
        return buildString {
            appendLine("/** The ${kind.noun} of [count] by the CLDR ${kind.cldr.version} rule for `$locale`. */")
            appendLine("private fun ${kind.function}(count: kotlin.Number): kotlin.String {")
            appendLine("    val o = io.github.octaviusframework.i18n.core.PluralOperands.of(count) ?: return \"other\"")
            appendLine("    return when {")
            for ((form, condition) in conditions) {
                appendLine("        ${condition.toKotlin()} -> \"$form\"")
            }
            appendLine("        else -> \"other\"")
            appendLine("    }")
            appendLine("}")
        }
    }

    companion object {
        private const val KEY = "pluralRule-count-"

        /** The text of each form's rule, with its samples, by form, from a locale's entry. */
        fun texts(entry: Map<String, String>): Map<String, String> =
            entry.filterKeys { it.startsWith(KEY) }.mapKeys { it.key.removePrefix(KEY) }

        /** Reads [locale]'s entry of rules of [kind]. */
        fun parse(locale: String, kind: PluralKind, entry: Map<String, String>): PluralRule {
            val texts = texts(entry)
            val conditions = Plural.FORMS
                .filter { it != "other" && it in texts }
                .associateWith { Condition.parse(texts.getValue(it).substringBefore('@')) }
            return PluralRule(locale, kind, conditions)
        }
    }
}
