package io.github.octaviusframework.i18n

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * A translation file as parsed, before the files of its language are put together.
 *
 * @property path Where the file was read from, as shown in error messages.
 * @property lang The language code taken from the file name.
 */
internal class TranslationFile(val path: String, val lang: String, val root: JsonObject)

/** One key: its message in each language, and the parameters its accessor takes. */
internal class TranslationKey(val path: String, val messages: Map<String, Message>, val signature: Signature)

/**
 * Every key a generator's files hold, in every language, the CLDR plural [rules] of the languages CLDR has, and
 * the [problems] that keep them from being generated. The keys are complete only when there are no problems.
 */
internal class Translations(
    val languages: List<String>,
    val keys: List<TranslationKey>,
    val rules: Map<String, PluralRule>,
    val problems: List<String>,
)

/**
 * Reads [files] into keys, listing every way they cannot be generated, one message per problem:
 * - a key defined in two files of one language,
 * - a key that is a translation in one file of a language and a group of keys in another,
 * - a key that some languages have and others lack, except in the [unreadable] languages, which have a file that
 *   could not be read and so lack whatever it held,
 * - a translation that is not a message, such as a `$plural` without `other`,
 * - arguments the languages of a key disagree on, as [Signature.of] describes,
 * - a language that has a plural with forms but no plural rule in CLDR.
 */
internal fun readTranslations(files: List<TranslationFile>, unreadable: Set<String> = emptySet()): Translations {
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
        if (lang in unreadable) continue
        for (key in allKeys) {
            if (key !in leaves) problems += "$lang has no `$key`"
        }
    }

    // key path -> language -> message
    val messages = linkedMapOf<String, MutableMap<String, Message>>()
    for ((lang, leaves) in byLang) {
        for ((key, leaf) in leaves) {
            val message = readMessage(leaf.first, Location(key, leaf.second), problems) ?: continue
            messages.getOrPut(key) { linkedMapOf() }[lang] = message
        }
    }

    val keys = allKeys.mapNotNull { key ->
        val byLanguage = messages[key] ?: return@mapNotNull null
        TranslationKey(key, byLanguage, Signature.of(key, byLanguage, problems))
    }

    val rules = linkedMapOf<String, PluralRule>()
    for (lang in byLang.keys) {
        val rule = CldrPlurals.ruleFor(lang)
        if (rule != null) {
            rules[lang] = rule
        } else if (messages.values.any { it[lang]?.usesPluralRule(Plural.FORMS.toSet()) == true }) {
            problems += "$lang has no plural rule in CLDR ${CldrPlurals.version}, and its plurals have forms to choose " +
                "from; a file is named after a language code, such as pl or pt-BR"
        }
    }

    return Translations(byLang.keys.toList(), keys, rules, problems.distinct())
}

/**
 * Calls [onLeaf] with the dotted path of every translation in [obj]. A message object is one translation; any
 * other object is a group of keys and is descended into.
 */
private fun collectLeaves(obj: JsonObject, prefix: String, onLeaf: (String, JsonElement) -> Unit) {
    for ((key, element) in obj) {
        val path = if (prefix.isEmpty()) key else "$prefix.$key"
        if (element is JsonObject && !isMessageObject(element)) {
            collectLeaves(element, path, onLeaf)
        } else {
            onLeaf(path, element)
        }
    }
}
