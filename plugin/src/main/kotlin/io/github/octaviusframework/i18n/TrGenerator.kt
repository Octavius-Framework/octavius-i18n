package io.github.octaviusframework.i18n

/**
 * Generates `{objectName}.kt`: the accessor object mirroring the keys, and the interface each language's
 * translations implement.
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

    /** A level of the accessor object: a nested object per group of keys, a function per key. */
    private class Group {
        val children = sortedMapOf<String, Any>()
    }

    /**
     * Renders the full `{objectName}.kt` source file for [translations], falling back to [defaultLang] when the
     * current language has no translations.
     */
    fun generate(translations: Translations, defaultLang: String): String {
        builder.clear()
        val interfaceName = "${objectName}Translations"

        // The IDE's style inspections, on what the generator writes on purpose: `public` for explicit API mode, full
        // names no type of the target package can shadow, `___` names no key can take, and interface functions named
        // after key paths. The `unused` inspection is left on: what it greys out is a key nothing calls
        appendLine(
            "@file:Suppress(\"RedundantVisibilityModifier\", \"RemoveRedundantQualifierName\", " +
                "\"ObjectPrivatePropertyName\", \"FunctionName\")"
        )
        appendLine()
        appendLine("package $packageName")
        appendLine()
        appendLine("/**")
        appendLine(" * Type-safe accessors for translations.")
        appendLine(" *")
        appendLine(" * This file is auto-generated. Do not edit manually.")
        appendLine(" *")
        appendLine(" * Usage:")
        appendLine(" * ```kotlin")
        appendLine(" * $objectName.Action.save()           // Get translation")
        appendLine(" * io.github.octaviusframework.i18n.core.OctaviusI18n.currentLanguage = \"en\"  // Switch language globally")
        appendLine(" * ```")
        appendLine(" */")
        appendLine("public object $objectName {")
        indentLevel++

        // No key or placeholder can be named `___…`: accessor names drop leading `_`, and an argument starts with a letter
        appendLine()
        appendLine("private val ___registry: kotlin.collections.Map<kotlin.String, $interfaceName> = mapOf(")
        indentLevel++
        for (lang in translations.languages) {
            appendLine("\"${escapeString(lang)}\" to $interfaceName${toPascalCase(lang)},")
        }
        indentLevel--
        appendLine(")")
        appendLine()

        appendLine("private val ___data: $interfaceName")
        indentLevel++
        appendLine("get() {")
        indentLevel++
        appendLine("val lang = io.github.octaviusframework.i18n.core.OctaviusI18n.currentLanguage")
        appendLine("return ___registry[lang] ?: ___registry[\"${escapeString(defaultLang)}\"] ?: ___registry.values.first()")
        indentLevel--
        appendLine("}")
        indentLevel--
        appendLine()

        // A property, so no key, which becomes a function or an object, can take its name
        appendLine("/** The number format of the current language, the one counts are written in, for writing another number. */")
        appendLine("public val numberFormat: io.github.octaviusframework.i18n.core.NumberFormat")
        indentLevel++
        appendLine("get() = ___data.numberFormat")
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
        generateGroup(root, translations.languages.firstOrNull { it == defaultLang } ?: translations.languages.first())

        indentLevel--
        appendLine("}")
        appendLine()

        appendLine("/** The translations of [$objectName] in one language. */")
        appendLine("internal interface $interfaceName {")
        indentLevel++
        appendLine("val numberFormat: io.github.octaviusframework.i18n.core.NumberFormat")
        for (key in translations.keys) {
            appendLine("fun ${memberName(key.path)}(${key.signature.declaration()}): kotlin.String")
        }
        indentLevel--
        appendLine("}")

        return builder.toString()
    }

    /** Emits one accessor per key in [group] and one nested `object` per group in it, sorted by key. */
    private fun generateGroup(group: Group, docLang: String) {
        for ((name, child) in group.children) {
            when (child) {
                is Group -> {
                    appendLine("public object ${escapeName(toPascalCase(name))} {")
                    indentLevel++
                    generateGroup(child, docLang)
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
                            "___data.${memberName(child.path)}(${child.signature.call()})"
                    )
                    appendLine()
                }
            }
        }
    }
}

/**
 * The name of [path]'s function in the translations interface: its accessor's name prefixed with the objects
 * it is nested in, such as `Legion_Orders_march`.
 */
internal fun memberName(path: String): String {
    val segments = path.split('.')
    val name = (segments.dropLast(1).map { toPascalCase(it) } + toCamelCase(segments.last())).joinToString("_")
    return escapeName(name)
}
