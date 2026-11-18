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
 * Every key a generator's files hold, in every language, each language's CLDR [rules] of the kinds CLDR has for
 * it, and the [problems] that keep them from being generated. The keys are complete only when there are no
 * problems.
 */
internal class Translations(
    val languages: List<String>,
    val keys: List<TranslationKey>,
    val rules: Map<String, Map<PluralKind, PluralRule>>,
    val problems: List<String>,
)

/**
 * Reads [files] into keys, listing every way they cannot be generated, one message per problem:
 * - no file at all, and no [unreadable] one either, since there would be no language to generate,
 * - a file name that is not a language code, such as `en.v2`,
 * - language codes that differ only in case or in `_` for `-`, such as `pl` and `PL`,
 * - a key defined in two files of one language,
 * - a key that is a translation in one file of a language and a group of keys in another,
 * - a key or group whose name the accessors cannot have, as [checkNames] describes,
 * - a key that some languages have and others lack, except in the [unreadable] languages, which have a file that
 *   could not be read and so lack whatever it held,
 * - a translation that is not a message, such as a `$plural` without `other`,
 * - arguments the languages of a key disagree on, as [Signature.of] describes,
 * - a language CLDR has no plural rule for, when one of its choices by a count has forms. A language CLDR has a
 *   plural rule but no ordinal rule for counts every ordinal as `other`, as CLDR's root does.
 */
internal fun readTranslations(files: List<TranslationFile>, unreadable: Set<String> = emptySet()): Translations {
    val problems = mutableListOf<String>()
    if (files.isEmpty() && unreadable.isEmpty()) {
        problems += "no translation file was found; a generator reads every src/**/i18n/*.json of its sourceProject " +
            "and of its subprojects"
    }

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

    // A language is found by its code with case and `_` or `-` ignored, so two such codes would be one language
    byLang.keys.groupBy { languageCode(it) }.values.filter { it.size > 1 }.forEach { codes ->
        problems += codes.joinToString(" and ") + " are one language code written two ways; the files of a language " +
            "are named alike"
    }

    // The code names the language's generated object, as `pt-BR` names `TrTranslationsPtBR`
    for (file in files) {
        if (!LANGUAGE_CODE.matches(file.lang)) {
            problems += "`${file.lang}`, the name of ${file.path}, is not a language code; a file is named after a " +
                "language code, such as pl or pt-BR"
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
    checkNames(allKeys, problems)
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

    val rules = linkedMapOf<String, Map<PluralKind, PluralRule>>()
    for (lang in byLang.keys) {
        // Reported above as not a language code, so not also as a language CLDR has no rule for
        if (!LANGUAGE_CODE.matches(lang)) continue
        if (PluralKind.CARDINAL.cldr.ruleFor(lang) != null) {
            rules[lang] = PluralKind.entries.mapNotNull { kind -> kind.cldr.ruleFor(lang)?.let { kind to it } }.toMap()
            continue
        }
        val everyForm = Plural.FORMS.toSet()
        if (messages.values.any { byLanguage -> PluralKind.entries.any { byLanguage[lang]?.usesRule(it) { everyForm } == true } }) {
            problems += "$lang has no plural rule in CLDR ${PluralKind.CARDINAL.cldr.version}, and its plurals have forms " +
                "to choose from; a file is named after a language code, such as pl or pt-BR"
        }
    }

    return Translations(byLang.keys.toList(), keys, rules, problems.distinct())
}

/** A language code as a file is named: letters, then parts of letters and digits after `-` or `_`, as in `es-419`. */
private val LANGUAGE_CODE = Regex("""[A-Za-z]+(?:[-_][A-Za-z0-9]+)*""")

/** An accessor's name, its `_` and `-` set aside: a letter, then letters and digits. */
private val KOTLIN_NAME = Regex("""\p{L}[\p{L}\p{Nd}]*""")

/** The functions without parameters every Kotlin object has, which a key's function would hide. */
private val ANY_FUNCTIONS = setOf("toString", "hashCode")

/**
 * Adds to [problems] every name in the key [paths] the accessors cannot have: one that is not a Kotlin name, two in
 * one group that are one Kotlin name, such as `fooBar` and `foo_bar`, and a key whose function the generated code
 * has already. A group is a property and an inner class `{Name}Group` of the group it is in, a key a function.
 */
private fun checkNames(paths: Collection<String>, problems: MutableList<String>) {
    // group path -> Kotlin name in that group -> the paths it is the name of
    val names = linkedMapOf<String, MutableMap<String, MutableSet<String>>>()
    val seen = mutableSetOf<String>()
    for (path in paths) {
        val segments = path.split('.')
        for (end in 1..segments.size) {
            val child = segments.subList(0, end).joinToString(".")
            if (!seen.add(child)) continue
            val group = segments.subList(0, end - 1).joinToString(".")
            val isKey = end == segments.size
            val name = if (isKey) toCamelCase(segments[end - 1]) else toPascalCase(segments[end - 1])
            if (!KOTLIN_NAME.matches(name)) {
                problems += "`$child` cannot be named in Kotlin; with `_` and `-` set aside, a name starts with a " +
                    "letter and holds only letters and digits"
                continue
            }
            if (isKey && name in ANY_FUNCTIONS) {
                problems += "`$child` cannot be a key: its function is `$name`, which every Kotlin object has"
            }
            if (isKey && group.isEmpty() && name == "inLanguage") {
                problems += "`$child` cannot be a key at the top level: its function is `inLanguage`, which the " +
                    "generated object has for another language"
            }
            val taken = names.getOrPut(group) { linkedMapOf() }
            for (kotlinName in if (isKey) listOf(name) else listOf(name, "${name}Group")) {
                taken.getOrPut(kotlinName) { sortedSetOf() } += child
            }
        }
    }
    // Two groups of one name share their class's name too, and are reported once
    val reported = mutableSetOf<Set<String>>()
    for (taken in names.values) {
        for ((kotlinName, children) in taken) {
            if (children.size < 2 || !reported.add(children)) continue
            val listed = children.map { "`$it`" }
            problems += listed.dropLast(1).joinToString(", ") + " and " + listed.last() +
                " are one Kotlin name, `$kotlinName`"
        }
    }
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
