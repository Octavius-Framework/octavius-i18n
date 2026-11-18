package io.github.octaviusframework.i18n

/** Text with placeholders such as `{name}`, each standing for the argument it names. */
internal class Template private constructor(val text: String) : Message {

    override fun collectUses(uses: MutableMap<String, MutableSet<Use>>) {
        for (match in PLACEHOLDER.findAll(text)) {
            uses.getOrPut(match.groupValues[1]) { mutableSetOf() } += Use.TEXT
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
        private val PLACEHOLDER = Regex("""\{(${ARGUMENT.pattern})\}""")
        private val NUMBERED = Regex("""\{\d+\}""")

        /** Reads [text], adding to [problems] the numbers in braces it has, which are not placeholders. */
        fun read(text: String, at: Location, problems: MutableList<String>): Template {
            val numbered = NUMBERED.findAll(text).map { it.value }.distinct().toList()
            if (numbered.isNotEmpty()) {
                val verb = if (numbered.size == 1) "is not a placeholder" else "are not placeholders"
                problems += at.problem(numbered.joinToString(", ") { "`$it`" } + " $verb; a placeholder is a name, such as `{count}`")
            }
            return Template(text)
        }
    }
}
