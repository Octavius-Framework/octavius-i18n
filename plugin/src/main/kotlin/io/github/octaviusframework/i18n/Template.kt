package io.github.octaviusframework.i18n

/**
 * Text with placeholders such as `{name}`, each standing for the argument it names. `{name, number}` marks the
 * argument as a number, written in the language's number format.
 */
internal class Template private constructor(val text: String) : Message {

    override fun collectUses(uses: MutableMap<String, MutableSet<Use>>) {
        for (match in PLACEHOLDER.findAll(text)) {
            val use = if (match.groups[2] != null) Use.NUMBER else Use.TEXT
            uses.getOrPut(match.groupValues[1]) { mutableSetOf() } += use
        }
    }

    override fun usesRule(kind: PluralKind, ruleForms: (PluralKind) -> Set<String>): Boolean = false

    override fun toKotlin(rendering: Rendering, indent: String): String {
        val literal = StringBuilder("\"")
        var end = 0
        for (match in PLACEHOLDER.findAll(text)) {
            literal.append(escapeString(text.substring(end, match.range.first)))
            literal.append("\${").append(rendering.inserts.getValue(match.groupValues[1])).append('}')
            end = match.range.last + 1
        }
        literal.append(escapeString(text.substring(end))).append('"')
        return literal.toString()
    }

    /** Whether the text has any placeholder. */
    fun hasPlaceholders(): Boolean = PLACEHOLDER.containsMatchIn(text)

    companion object {
        /** `{name}`, or `{name, format}` with the format in the second group. */
        private val PLACEHOLDER = Regex("""\{(${ARGUMENT.pattern})(?:\s*,([^{}]*))?\}""")
        private val NUMBERED = Regex("""\{\d+\}""")

        /**
         * Reads [text], adding to [problems] what is in braces but is not a placeholder: a number, or a name
         * followed by a format other than `number`.
         */
        fun read(text: String, at: Location, problems: MutableList<String>): Template {
            val numbered = NUMBERED.findAll(text).map { it.value }.distinct().toList()
            if (numbered.isNotEmpty()) {
                val verb = if (numbered.size == 1) "is not a placeholder" else "are not placeholders"
                problems += at.problem(numbered.joinToString(", ") { "`$it`" } + " $verb; a placeholder is a name, such as `{count}`")
            }
            val formatted = PLACEHOLDER.findAll(text)
                .filter { match -> match.groups[2]?.value?.trim()?.let { it != "number" } == true }
                .map { it.value }.distinct().toList()
            if (formatted.isNotEmpty()) {
                val verb = if (formatted.size == 1) "is not a placeholder" else "are not placeholders"
                problems += at.problem(formatted.joinToString(", ") { "`$it`" } + " $verb; a placeholder is `{name}` or `{name, number}`")
            }
            return Template(text)
        }
    }
}
