package io.github.octaviusframework.i18n

/** A parameter of a key's accessor: the [argument] it carries, its Kotlin [name] and [type]. */
internal class Parameter(val argument: String, val name: String, val type: String)

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
         * languages disagree on: one that chooses a plural form in one place and a case in another, and one only
         * inserted into the text that a language leaves out.
         */
        fun of(key: String, messages: Map<String, Message>, problems: MutableList<String>): Signature {
            val usesByLang = messages.mapValues { (_, message) ->
                mutableMapOf<String, MutableSet<Use>>().also { message.collectUses(it) }
            }
            val arguments = usesByLang.values.flatMapTo(linkedSetOf()) { it.keys }

            val parameters = arguments.map { argument ->
                fun languagesWith(use: Use) = usesByLang.filterValues { use in it[argument].orEmpty() }.keys
                val plural = languagesWith(Use.PLURAL)
                val select = languagesWith(Use.SELECT)
                if (plural.isNotEmpty() && select.isNotEmpty()) {
                    problems += "`$key` chooses by `$argument` as a `\$plural` in ${plural.joinToString(", ")} " +
                        "and as a `\$select` in ${select.joinToString(", ")}"
                }
                if (plural.isEmpty() && select.isEmpty()) {
                    val without = usesByLang.filterValues { argument !in it }.keys
                    if (without.isNotEmpty()) {
                        val with = usesByLang.keys - without
                        problems += "`$key` uses {$argument} in ${with.joinToString(", ")} but not in ${without.joinToString(", ")}"
                    }
                }
                val type = when {
                    plural.isNotEmpty() -> "kotlin.Number"
                    select.isNotEmpty() -> "kotlin.String"
                    else -> "kotlin.Any"
                }
                Parameter(argument, escapeName(argument), type)
            }

            return Signature(parameters.sortedWith(compareBy({ it.type == "kotlin.Any" }, { it.argument })))
        }
    }
}
