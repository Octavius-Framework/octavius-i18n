package io.github.octaviusframework.i18n

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * One key's translation in one language: a [Template], or a [Plural] or [Select] choosing between messages.
 */
internal sealed interface Message {

    /** Adds to [uses] what this message does with each argument. */
    fun collectUses(uses: MutableMap<String, MutableSet<Use>>)

    /**
     * Whether rendering this message asks the language's plural rule for a form, when the rule names [ruleForms]. A
     * plural whose only forms the rule could name is `other` does not ask.
     */
    fun usesPluralRule(ruleForms: Set<String>): Boolean

    /**
     * A Kotlin expression rendering this message, with [parameters] naming each argument's parameter, in a
     * language whose plural rule names [ruleForms]. Lines after the first start with [indent].
     */
    fun toKotlin(parameters: Map<String, String>, ruleForms: Set<String>, indent: String): String
}

/** What a message does with an argument: inserts it into the text, or chooses by it. */
internal enum class Use { TEXT, PLURAL, SELECT }

/** Where a message sits, for problems to name: its key, its file, and the branches leading to it. */
internal class Location(private val key: String, private val file: String, private val branches: List<String> = emptyList()) {

    fun branch(name: String) = Location(key, file, branches + name)

    fun problem(what: String): String {
        val under = if (branches.isEmpty()) "" else ", under " + branches.joinToString(" → ") { "`$it`" }
        return "`$key` in $file$under: $what"
    }
}

/** An argument's name: a letter followed by letters, digits and `_`. */
internal val ARGUMENT = Regex("""[A-Za-z][A-Za-z0-9_]*""")

/** Whether [obj] is one translation rather than a group of keys: an object with a `$` key. */
internal fun isMessageObject(obj: JsonObject): Boolean = obj.keys.any { it.startsWith('$') }

/**
 * Reads [element] as a message, adding to [problems] everything that keeps it from being one.
 * Returns null when it cannot be read.
 */
internal fun readMessage(element: JsonElement, at: Location, problems: MutableList<String>): Message? {
    return when {
        element !is JsonObject -> Template.read((element as? JsonPrimitive)?.content ?: element.toString(), at, problems)
        isMessageObject(element) -> readChoice(element, at, problems)
        else -> {
            problems += at.problem("a branch is a string, or an object with `\$plural` or `\$select`")
            null
        }
    }
}

private fun readChoice(obj: JsonObject, at: Location, problems: MutableList<String>): Message? {
    val unknown = obj.keys.filter { it.startsWith('$') && it != "\$plural" && it != "\$select" }
    if (unknown.isNotEmpty()) {
        problems += at.problem(unknown.joinToString(", ") { "`$it`" } + " is not `\$plural` or `\$select`")
    }
    if ("\$plural" in obj && "\$select" in obj) {
        problems += at.problem("`\$plural` and `\$select` are in one object; put one in the branches of the other")
        return null
    }
    return when {
        "\$plural" in obj -> Plural.read(obj, at, problems)
        "\$select" in obj -> Select.read(obj, at, problems)
        else -> null
    }
}

/** Reads the argument a choice names under [discriminator], such as `"$plural": "count"`. */
internal fun readArgument(obj: JsonObject, discriminator: String, at: Location, problems: MutableList<String>): String? {
    val name = (obj[discriminator] as? JsonPrimitive)?.takeIf { it.isString }?.content
    if (name == null || !ARGUMENT.matches(name)) {
        problems += at.problem("`$discriminator` names the argument it chooses by, such as \"count\" or \"gender\"")
        return null
    }
    return name
}
