package io.github.octaviusframework.i18n

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * A translation file as parsed, before the files of its language are merged.
 *
 * @property path Where the file was read from, as shown in error messages.
 * @property lang The language code taken from the file name.
 */
internal class TranslationFile(val path: String, val lang: String, val root: JsonObject)

/**
 * What a key generates: a plain accessor or a plural one, and the placeholders its text uses.
 * For a plural, `{0}` is the count and is left out.
 */
private data class KeyShape(val plural: Boolean, val placeholders: Set<Int>) {
    override fun toString(): String {
        val kind = if (plural) "a plural" else "a string"
        return when {
            placeholders.isNotEmpty() -> "$kind using " + placeholders.sorted().joinToString(", ") { "{$it}" }
            plural -> kind
            else -> "$kind with no placeholders"
        }
    }
}

/**
 * Lists every way [files] disagree with each other, one message per problem:
 * - a key defined in two files of one language,
 * - a key that is a translation in one file of a language and a group of keys in another,
 * - a key that some languages have and others lack,
 * - a key that is a string in one language and a plural in another, or uses other placeholders.
 *
 * Plural forms are not compared: languages need different ones. An empty list means the
 * accessors can be generated.
 */
internal fun findInconsistencies(files: List<TranslationFile>): List<String> {
    val problems = mutableListOf<String>()

    // language -> key path -> (translation, file it came from)
    val byLang = linkedMapOf<String, MutableMap<String, Pair<JsonElement, String>>>()
    for (file in files) {
        val leaves = byLang.getOrPut(file.lang) { linkedMapOf() }
        collectLeaves(file.root, "") { key, leaf ->
            val earlier = leaves[key]
            if (earlier != null) {
                problems += "`$key` is defined in both ${earlier.second} and ${file.path}"
            } else {
                leaves[key] = leaf to file.path
            }
        }
    }

    for (leaves in byLang.values) {
        for ((key, leaf) in leaves) {
            var dot = key.indexOf('.')
            while (dot >= 0) {
                val group = key.substring(0, dot)
                leaves[group]?.let { problems += "`$group` is a translation in ${it.second} and a group of keys in ${leaf.second}" }
                dot = key.indexOf('.', dot + 1)
            }
        }
    }

    val allKeys = byLang.values.flatMapTo(sortedSetOf()) { it.keys }
    for ((lang, leaves) in byLang) {
        for (key in allKeys) {
            if (key !in leaves) problems += "$lang has no `$key`"
        }
    }

    for (key in allKeys) {
        val langsByShape = byLang.entries
            .mapNotNull { (lang, leaves) -> leaves[key]?.let { shapeOf(it.first) to lang } }
            .groupBy({ it.first }, { it.second })
        if (langsByShape.size > 1) {
            problems += "`$key` is not the same in every language: " +
                langsByShape.entries.joinToString("; ") { (shape, langs) -> "$shape in ${langs.joinToString(", ")}" }
        }
    }

    return problems.distinct()
}

/**
 * Calls [onLeaf] with the dotted path of every translation in [obj]. A plural form group is one
 * translation; any other object is a group of keys and is descended into.
 */
private fun collectLeaves(obj: JsonObject, prefix: String, onLeaf: (String, JsonElement) -> Unit) {
    for ((key, element) in obj) {
        val path = if (prefix.isEmpty()) key else "$prefix.$key"
        if (element is JsonObject && !isPluralObject(element)) {
            collectLeaves(element, path, onLeaf)
        } else {
            onLeaf(path, element)
        }
    }
}

/** The shape of the accessor generated for [leaf], read the same way the generator reads it. */
private fun shapeOf(leaf: JsonElement): KeyShape {
    return if (leaf is JsonObject) {
        KeyShape(true, leaf.values.flatMapTo(sortedSetOf()) { placeholdersIn((it as JsonPrimitive).content) } - 0)
    } else {
        KeyShape(false, placeholdersIn((leaf as? JsonPrimitive)?.content ?: leaf.toString()))
    }
}

private fun placeholdersIn(text: String): Set<Int> {
    return PARAM_REGEX.findAll(text).mapTo(sortedSetOf()) { it.groupValues[1].toInt() }
}
