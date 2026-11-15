package io.github.octaviusframework.i18n

import kotlinx.serialization.json.JsonObject

/**
 * Chooses a message by a count: an exact value first (`=0`), then the form the language's plural rule names,
 * then `other`. [forms] always holds `other`.
 */
internal class Plural(
    private val argument: String,
    private val exact: List<Pair<Double, Message>>,
    private val forms: Map<String, Message>,
) : Message {

    override fun collectUses(uses: MutableMap<String, MutableSet<Use>>) {
        uses.getOrPut(argument) { mutableSetOf() } += Use.PLURAL
        exact.forEach { it.second.collectUses(uses) }
        forms.values.forEach { it.collectUses(uses) }
    }

    override fun usesPluralRule(): Boolean =
        forms.size > 1 || exact.any { it.second.usesPluralRule() } || forms.values.any { it.usesPluralRule() }

    override fun toKotlin(parameters: Map<String, String>, indent: String): String {
        val count = parameters.getValue(argument)
        val inner = "$indent    "
        if (exact.isEmpty()) return byForm(count, parameters, indent)
        return buildString {
            appendLine("when {")
            for ((value, message) in exact) {
                appendLine("$inner$count.toDouble() == $value -> ${message.toKotlin(parameters, inner)}")
            }
            appendLine("${inner}else -> ${byForm(count, parameters, inner)}")
            append("$indent}")
        }
    }

    private fun byForm(count: String, parameters: Map<String, String>, indent: String): String {
        val other = forms.getValue("other")
        if (forms.size == 1) return other.toKotlin(parameters, indent)
        val inner = "$indent    "
        return buildString {
            appendLine("when ($FORM_FUNCTION($count)) {")
            for (form in FORMS) {
                if (form == "other") continue
                val message = forms[form] ?: continue
                appendLine("$inner\"$form\" -> ${message.toKotlin(parameters, inner)}")
            }
            appendLine("${inner}else -> ${other.toKotlin(parameters, inner)}")
            append("$indent}")
        }
    }

    companion object {
        /** The plural forms, in CLDR order. */
        val FORMS = listOf("zero", "one", "two", "few", "many", "other")

        /** The private function in each language's file that names the form of a count. */
        const val FORM_FUNCTION = "___form"

        private val EXACT = Regex("""=-?\d+(\.\d+)?""")

        /** Reads `{"$plural": "count", "one": …, "other": …}`. */
        fun read(obj: JsonObject, at: Location, problems: MutableList<String>): Plural? {
            val argument = readArgument(obj, "\$plural", at, problems)
            val exact = mutableListOf<Pair<Double, Message>>()
            val forms = mutableMapOf<String, Message>()
            for ((key, value) in obj) {
                if (key.startsWith('$')) continue
                when {
                    key in FORMS -> readMessage(value, at.branch(key), problems)?.let { forms[key] = it }
                    EXACT.matches(key) -> readMessage(value, at.branch(key), problems)?.let { exact += key.substring(1).toDouble() to it }
                    else -> problems += at.problem(
                        "`$key` is not a form; a `\$plural` takes ${FORMS.joinToString(", ")} and exact values such as `=0`"
                    )
                }
            }
            if ("other" !in obj) problems += at.problem("the `\$plural` has no `other`")
            return if (argument != null && "other" in forms) Plural(argument, exact, forms) else null
        }
    }
}
