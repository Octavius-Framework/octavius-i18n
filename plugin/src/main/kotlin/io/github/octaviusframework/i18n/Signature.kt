package io.github.octaviusframework.i18n

/**
 * A parameter of a key's accessor: the [argument] it carries, its Kotlin [name] and [type], and whether some
 * language [chooses] its text by it.
 */
internal class Parameter(val argument: String, val name: String, val type: String, val chooses: Boolean)

/**
 * The parameters of one key's accessor, the same in every language: every argument any language uses, the ones
 * that choose first, each group in alphabetical order.
 */
internal class Signature(val parameters: List<Parameter>) {

    fun declaration(): String = parameters.joinToString(", ") { "${it.name}: ${it.type}" }

    fun call(): String = parameters.joinToString(", ") { it.name }

    fun names(): Map<String, String> = parameters.associate { it.argument to it.name }

    companion object {
        /**
         * The signature of [key] from its [messages] by language, adding to [problems] every argument the
         * languages disagree on: one that chooses one way in one place and another way elsewhere, such as a plural
         * form here and a case there, one that is a number in one place and the case of a `$select` in another,
         * and one only inserted into the text that a language leaves out.
         */
        fun of(key: String, messages: Map<String, Message>, problems: MutableList<String>): Signature {
            val usesByLang = messages.mapValues { (_, message) ->
                mutableMapOf<String, MutableSet<Use>>().also { message.collectUses(it) }
            }
            val arguments = usesByLang.values.flatMapTo(linkedSetOf()) { it.keys }

            val parameters = arguments.map { argument ->
                // Each way the argument is used besides being inserted as it prints, with the languages using it so
                val ways = Use.entries.filter { it != Use.TEXT }
                    .associateWith { use -> usesByLang.filterValues { use in it[argument].orEmpty() }.keys }
                    .filterValues { it.isNotEmpty() }
                val choices = ways.filterKeys { it.discriminator != null }
                if (choices.size > 1) {
                    problems += "`$key` chooses by `$argument` as " + choices.entries.joinToString(" and as ") { (use, langs) ->
                        "a `${use.discriminator}` in ${langs.joinToString(", ")}"
                    }
                } else if (Use.SELECT in ways && Use.NUMBER in ways) {
                    problems += "`$key` writes `$argument` as a number in ${ways.getValue(Use.NUMBER).joinToString(", ")} " +
                        "but chooses by it as a `\$select` in ${ways.getValue(Use.SELECT).joinToString(", ")}"
                }
                if (choices.isEmpty()) {
                    val without = usesByLang.filterValues { argument !in it }.keys
                    if (without.isNotEmpty()) {
                        val with = usesByLang.keys - without
                        problems += "`$key` uses {$argument} in ${with.joinToString(", ")} but not in ${without.joinToString(", ")}"
                    }
                }
                val type = when {
                    Use.SELECT in ways -> "kotlin.String"
                    ways.keys.any { it.number } -> "kotlin.Number"
                    else -> "kotlin.Any"
                }
                Parameter(argument, escapeName(argument), type, choices.isNotEmpty())
            }

            return Signature(parameters.sortedWith(compareBy({ !it.chooses }, { it.argument })))
        }
    }
}
