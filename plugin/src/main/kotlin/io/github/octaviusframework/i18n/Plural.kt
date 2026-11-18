package io.github.octaviusframework.i18n

import kotlinx.serialization.json.JsonObject

/**
 * Chooses a message by a count: an exact value first (`=0`), then the form the language's rule of [kind] names,
 * then `other`. A `$plural` counts how many and a `$ordinal` which in order. [forms] always holds `other`. A form
 * the language's rule never names is left out of the code.
 */
internal class Plural(
    private val argument: String,
    private val kind: PluralKind,
    private val exact: List<Pair<Double, Message>>,
    private val forms: Map<String, Message>,
) : Message {

    override fun collectUses(uses: MutableMap<String, MutableSet<Use>>) {
        uses.getOrPut(argument) { mutableSetOf() } += kind.use
        exact.forEach { it.second.collectUses(uses) }
        forms.values.forEach { it.collectUses(uses) }
    }

    /** The forms this choice has, besides `other`, that a rule naming [ruleForms] can choose, in CLDR order. */
    private fun reachable(ruleForms: Set<String>): List<String> = FORMS.filter { it != "other" && it in forms && it in ruleForms }

    override fun usesRule(kind: PluralKind, ruleForms: (PluralKind) -> Set<String>): Boolean =
        kind == this.kind && reachable(ruleForms(kind)).isNotEmpty() ||
            exact.any { it.second.usesRule(kind, ruleForms) } ||
            forms.values.any { it.usesRule(kind, ruleForms) }

    override fun toKotlin(parameters: Map<String, String>, ruleForms: (PluralKind) -> Set<String>, indent: String): String {
        val count = parameters.getValue(argument)
        val inner = "$indent    "
        if (exact.isEmpty()) return byForm(count, parameters, ruleForms, indent)
        return buildString {
            appendLine("when {")
            for ((value, message) in exact) {
                appendLine("$inner$count.toDouble() == $value -> ${message.toKotlin(parameters, ruleForms, inner)}")
            }
            appendLine("${inner}else -> ${byForm(count, parameters, ruleForms, inner)}")
            append("$indent}")
        }
    }

    private fun byForm(count: String, parameters: Map<String, String>, ruleForms: (PluralKind) -> Set<String>, indent: String): String {
        val other = forms.getValue("other")
        val reachable = reachable(ruleForms(kind))
        if (reachable.isEmpty()) return other.toKotlin(parameters, ruleForms, indent)
        val inner = "$indent    "
        return buildString {
            appendLine("when (${kind.function}($count)) {")
            for (form in reachable) {
                appendLine("$inner\"$form\" -> ${forms.getValue(form).toKotlin(parameters, ruleForms, inner)}")
            }
            appendLine("${inner}else -> ${other.toKotlin(parameters, ruleForms, inner)}")
            append("$indent}")
        }
    }

    companion object {
        /** The plural forms, in CLDR order. */
        val FORMS = listOf("zero", "one", "two", "few", "many", "other")

        private val EXACT = Regex("""=-?\d+(\.\d+)?""")

        /** Reads `{"$plural": "count", "one": …, "other": …}`, or the same with `$ordinal` for a [kind] of ordinal. */
        fun read(obj: JsonObject, kind: PluralKind, at: Location, problems: MutableList<String>): Plural? {
            val discriminator = kind.discriminator
            val argument = readArgument(obj, discriminator, at, problems)
            val exact = mutableListOf<Pair<Double, Message>>()
            val forms = mutableMapOf<String, Message>()
            for ((key, value) in obj) {
                if (key.startsWith('$')) continue
                when {
                    key in FORMS -> readMessage(value, at.branch(key), problems)?.let { forms[key] = it }
                    EXACT.matches(key) -> readMessage(value, at.branch(key), problems)?.let { exact += key.substring(1).toDouble() to it }
                    else -> problems += at.problem(
                        "`$key` is not a form; a `$discriminator` takes ${FORMS.joinToString(", ")} and exact values such as `=0`"
                    )
                }
            }
            if ("other" !in obj) problems += at.problem("the `$discriminator` has no `other`")
            return if (argument != null && "other" in forms) Plural(argument, kind, exact, forms) else null
        }
    }
}
