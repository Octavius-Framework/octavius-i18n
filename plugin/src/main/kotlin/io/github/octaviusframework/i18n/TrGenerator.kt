package io.github.octaviusframework.i18n

/**
 * Generates `{objectName}.kt`: the accessor object for the current language, the accessor class it is an instance
 * of, which mirrors the keys, and the interface each language's translations implement.
 */
internal class TrGenerator(private val packageName: String, private val objectName: String) {
    private val builder = StringBuilder()
    private var indentLevel = 0

    private fun indent() = "    ".repeat(indentLevel)

    private fun appendLine(line: String = "") {
        if (line.isEmpty()) {
            builder.appendLine()
        } else {
            builder.appendLine("${indent()}$line")
        }
    }

    /** A level of the accessor class: an inner class per group of keys, a function per key. */
    private class Group {
        val children = sortedMapOf<String, Any>()
    }

    /**
     * Renders the full `{objectName}.kt` source file for [translations], falling back to the language
     * [defaultLang] finds, or to the first one when it finds none, for a language that has no translations. The
     * accessors' documentation shows that language's templates.
     */
    fun generate(translations: Translations, defaultLang: String): String {
        builder.clear()
        val fallback = findLanguage(defaultLang, translations.languages) ?: translations.languages.first()
        val interfaceName = "${objectName}Translations"
        val accessorsName = "${objectName}Accessors"

        // The IDE's style inspections, on what the generator writes on purpose: `public` for explicit API mode, full
        // names no type of the target package can shadow, `___` names no key can take, and functions and
        // properties named after keys. The `unused` inspection is left on: what it greys out is a key nothing calls
        appendLine(
            "@file:Suppress(\"RedundantVisibilityModifier\", \"RemoveRedundantQualifierName\", " +
                "\"ObjectPrivatePropertyName\", \"PrivatePropertyName\", \"PropertyName\", \"FunctionName\")"
        )
        appendLine()
        appendLine("package $packageName")
        appendLine()
        appendLine("/**")
        appendLine(" * Type-safe accessors for translations, in the current language.")
        appendLine(" *")
        appendLine(" * This file is auto-generated. Do not edit manually.")
        appendLine(" *")
        appendLine(" * Usage:")
        appendLine(" * ```kotlin")
        appendLine(" * $objectName.Action.save()                     // in the current language")
        appendLine(" * $objectName.inLanguage(\"pl\").Action.save()     // in Polish, whatever the current language is")
        appendLine(" * io.github.octaviusframework.i18n.core.OctaviusI18n.currentLanguage = \"en\"  // Switch language globally")
        appendLine(" * ```")
        appendLine(" */")
        appendLine("public object $objectName : $accessorsName(::___current) {")
        indentLevel++
        appendLine()
        appendLine("/**")
        appendLine(" * The translations in [language], found as the current language is, without changing the current language:")
        appendLine(" * for a text in another language than the current one, as a server answering each user in theirs needs.")
        appendLine(" * Every code that finds one language gives the same object.")
        appendLine(" */")
        appendLine("public fun inLanguage(language: kotlin.String): $accessorsName = ___find(language).___accessors")
        indentLevel--
        appendLine("}")
        appendLine()

        appendLine("/** The accessors of [$objectName] in one language: a property per group of keys, a function per key. */")
        appendLine("public open class $accessorsName internal constructor(private val ___data: () -> $interfaceName) {")
        indentLevel++
        appendLine()
        // A property, so no key, which becomes a function or a group, can take its name
        appendLine("/** The number format of the language, the one counts are written in, for writing another number. */")
        appendLine("public val numberFormat: io.github.octaviusframework.i18n.core.NumberFormat")
        indentLevel++
        appendLine("get() = ___data().numberFormat")
        indentLevel--
        appendLine()

        val root = Group()
        for (key in translations.keys) {
            val segments = key.path.split('.')
            var group = root
            for (segment in segments.dropLast(1)) {
                group = group.children.getOrPut(segment) { Group() } as Group
            }
            group.children[segments.last()] = key
        }
        generateGroup(root, "", fallback)

        indentLevel--
        appendLine("}")
        appendLine()

        // No key or placeholder can be named `___…`: accessor names drop leading `_`, and an argument starts with a letter
        // A branch per language, so finding one loads that language's object and no other
        appendLine("/** The translations of the language whose code, in lower case with `-` between its parts, is [code]. */")
        appendLine("private fun ___language(code: kotlin.String): $interfaceName? = when (code) {")
        indentLevel++
        for (lang in translations.languages) {
            appendLine("\"${escapeString(languageCode(lang))}\" -> $interfaceName${toPascalCase(lang)}")
        }
        appendLine("else -> null")
        indentLevel--
        appendLine("}")
        appendLine()

        appendLine("/** The translations of [code], or of the code with its last part cut off; case and `_` or `-` do not matter. */")
        appendLine("private fun ___lookup(code: kotlin.String): $interfaceName? {")
        indentLevel++
        appendLine("___language(code)?.let { return it }")
        appendLine("var shorter = code.lowercase().replace('_', '-')")
        appendLine("while (true) {")
        indentLevel++
        appendLine("___language(shorter)?.let { return it }")
        appendLine("val cut = shorter.lastIndexOf('-')")
        appendLine("if (cut < 0) return null")
        appendLine("shorter = shorter.substring(0, cut)")
        indentLevel--
        appendLine("}")
        indentLevel--
        appendLine("}")
        appendLine()

        appendLine("/** The translations of [language], or of the fallback language. */")
        appendLine("private fun ___find(language: kotlin.String): $interfaceName =")
        indentLevel++
        appendLine("___lookup(language) ?: $interfaceName${toPascalCase(fallback)}")
        indentLevel--
        appendLine()

        // Volatile, so a thread that sees the pair sees its fields: Kotlin/Native has no final field semantics, and
        // without it may publish the pair before its constructor's writes. Threads that race may each find the
        // language, and whichever pair stays is right for its own code
        appendLine("/** The current language's code and its translations, as last found, so a code is found again only after a switch. */")
        appendLine("@kotlin.concurrent.Volatile")
        appendLine("private var ___found: kotlin.Pair<kotlin.String, $interfaceName>? = null")
        appendLine()

        appendLine("private fun ___current(): $interfaceName {")
        indentLevel++
        appendLine("val code = io.github.octaviusframework.i18n.core.OctaviusI18n.currentLanguage")
        appendLine("___found?.let { if (it.first == code) return it.second }")
        appendLine("return ___find(code).also { ___found = kotlin.Pair(code, it) }")
        indentLevel--
        appendLine("}")
        appendLine()

        appendLine("/** The translations of [$objectName] in one language. */")
        appendLine("internal interface $interfaceName {")
        indentLevel++
        appendLine("val numberFormat: io.github.octaviusframework.i18n.core.NumberFormat")
        appendLine("val ___accessors: $accessorsName")
        for (key in translations.keys) {
            appendLine("fun ${memberName(key.path)}(${key.signature.declaration()}): kotlin.String")
        }
        indentLevel--
        appendLine("}")

        return builder.toString()
    }

    /**
     * Emits one accessor per key in [group], the group at [path], and one property with an inner class per group in
     * it, sorted by key.
     */
    private fun generateGroup(group: Group, path: String, docLang: String) {
        for ((name, child) in group.children) {
            when (child) {
                is Group -> {
                    val childPath = if (path.isEmpty()) name else "$path.$name"
                    // A class may not share the property's name, and no group's name holds `_`, so no sibling's
                    // property can take the class's
                    val propertyName = escapeName(toPascalCase(name))
                    val className = "${toPascalCase(name)}_Group"
                    val fieldName = "___${toPascalCase(name)}"
                    // Made on first use, so a group's class is loaded only once the group is called. Volatile, as
                    // `___found` is, so no thread sees the group before its reference to the accessors; threads
                    // that race may each make one, and any of them serves
                    appendLine("/** The keys under `$childPath`. */")
                    appendLine("public val $propertyName: $className")
                    indentLevel++
                    appendLine("get() = $fieldName ?: $className().also { $fieldName = it }")
                    indentLevel--
                    appendLine()
                    appendLine("@kotlin.concurrent.Volatile")
                    appendLine("private var $fieldName: $className? = null")
                    appendLine()
                    appendLine("public inner class $className internal constructor() {")
                    indentLevel++
                    generateGroup(child, childPath, docLang)
                    indentLevel--
                    appendLine("}")
                    appendLine()
                }

                is TranslationKey -> {
                    val shown = (child.messages[docLang] as? Template)?.takeIf { it.hasPlaceholders() }
                    val template = shown?.let { " - Template: `${it.text.replace("*/", "* /")}`" } ?: ""
                    appendLine("/** `${child.path}`$template */")
                    appendLine(
                        "public fun ${escapeName(toCamelCase(name))}(${child.signature.declaration()}): kotlin.String = " +
                            "___data().${memberName(child.path)}(${child.signature.call()})"
                    )
                    appendLine()
                }
            }
        }
    }
}

/**
 * The name of [path]'s function in the translations interface: its accessor's name prefixed with the groups
 * it is nested in, such as `Legion_Orders_march`.
 */
internal fun memberName(path: String): String {
    val segments = path.split('.')
    val name = (segments.dropLast(1).map { toPascalCase(it) } + toCamelCase(segments.last())).joinToString("_")
    return escapeName(name)
}
