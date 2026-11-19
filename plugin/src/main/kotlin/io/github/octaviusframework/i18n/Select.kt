package io.github.octaviusframework.i18n

import kotlinx.serialization.json.JsonObject

/** Chooses a message by a string argument: the case it equals, or `other`. [cases] always holds `other`. */
internal class Select(private val argument: String, private val cases: Map<String, Message>) : Message {

    override fun collectUses(uses: MutableMap<String, MutableSet<Use>>) {
        uses.getOrPut(argument) { mutableSetOf() } += Use.SELECT
        cases.values.forEach { it.collectUses(uses) }
    }

    override fun usesRule(kind: PluralKind, ruleForms: (PluralKind) -> Set<String>): Boolean =
        cases.values.any { it.usesRule(kind, ruleForms) }

    override fun toKotlin(rendering: Rendering, indent: String): String {
        val other = cases.getValue("other")
        if (cases.size == 1) return other.toKotlin(rendering, indent)
        val inner = "$indent    "
        return buildString {
            appendLine("when (${rendering.parameters.getValue(argument)}) {")
            for ((case, message) in cases) {
                if (case == "other") continue
                appendLine("$inner\"${escapeString(case)}\" -> ${message.toKotlin(rendering, inner)}")
            }
            appendLine("${inner}else -> ${other.toKotlin(rendering, inner)}")
            append("$indent}")
        }
    }

    companion object {
        /** Reads `{"$select": "gender", "female": …, "other": …}`. */
        fun read(obj: JsonObject, at: Location, problems: MutableList<String>): Select? {
            val argument = readArgument(obj, "\$select", at, problems)
            val cases = linkedMapOf<String, Message>()
            for ((key, value) in obj) {
                if (key.startsWith('$')) continue
                readMessage(value, at.branch(key), problems)?.let { cases[key] = it }
            }
            if ("other" !in obj) problems += at.problem("the `\$select` has no `other`")
            return if (argument != null && "other" in cases) Select(argument, cases) else null
        }
    }
}
