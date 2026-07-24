package io.github.octaviusframework.i18n


/**
 * Generates the main Tr.kt file with registry and type-safe accessors.
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

    fun generate(entries: Map<String, TranslationEntry>, defaultLang: String, allLangs: List<String>): String {
        builder.clear()

        appendLine("@file:Suppress(\"unused\", \"RedundantVisibilityModifier\")")
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

        appendLine()
        appendLine("private val ___registry = mutableMapOf<kotlin.String, io.github.octaviusframework.i18n.core.TranslationData>()")
        appendLine()

        // Init block
        appendLine("init {")
        indentLevel++
        allLangs.forEach { lang ->
            val langPascal = toPascalCase(lang)
            builder.appendLine("        ___registry[\"$lang\"] = ${objectName}Translations$langPascal")
        }
        indentLevel--
        appendLine("}")
        appendLine()

        // Private data accessor
        appendLine("private val ___data: io.github.octaviusframework.i18n.core.TranslationData")
        indentLevel++
        appendLine("get() {")
        indentLevel++
        appendLine("val lang = io.github.octaviusframework.i18n.core.OctaviusI18n.currentLanguage")
        appendLine("return ___registry[lang] ?: error(\"Language \\\"${'$'}lang\\\" not registered in this module\")")
        indentLevel--
        appendLine("}")
        indentLevel--
        appendLine()

        // Type-safe accessors
        generateEntries(entries, "")

        indentLevel--
        appendLine("}")
        appendLine()

        return builder.toString()
    }

    private fun generateEntries(entries: Map<String, TranslationEntry>, keyPrefix: String) {
        val sortedEntries = entries.entries.sortedBy { it.key }

        for ((key, entry) in sortedEntries) {
            val fullKey = if (keyPrefix.isEmpty()) key else "$keyPrefix.$key"

            when (entry) {
                is TranslationEntry.Simple -> {
                    val funcName = escapeName(toCamelCase(key))
                    appendLine("/** `$fullKey` */")
                    appendLine("public fun $funcName(): kotlin.String = io.github.octaviusframework.i18n.core.OctaviusI18n.lookup(___data, \"$fullKey\")")
                    appendLine()
                }

                is TranslationEntry.Parameterized -> {
                    val funcName = escapeName(toCamelCase(key))
                    val params = (0 until entry.paramCount).joinToString(", ") { "arg$it: kotlin.Any" }
                    val args = (0 until entry.paramCount).joinToString(", ") { "arg$it" }
                    appendLine("/** `$fullKey` - Template: `${entry.value}` */")
                    appendLine("public fun $funcName($params): kotlin.String = io.github.octaviusframework.i18n.core.OctaviusI18n.lookup(___data, \"$fullKey\", $args)")
                    appendLine()
                }

                is TranslationEntry.Plural -> {
                    val funcName = escapeName(toCamelCase(key))
                    // Check if plural forms have parameters (besides {0} which is count)
                    val allForms = entry.forms.values.joinToString(" ")
                    val extraParams = PARAM_REGEX.findAll(allForms)
                        .map { it.groupValues[1].toInt() }
                        .filter { it > 0 }
                        .toList()

                    if (extraParams.isNotEmpty()) {
                        val maxParam = extraParams.max()
                        val params = (1..maxParam).joinToString(", ") { "arg$it: kotlin.Any" }
                        val args = (1..maxParam).joinToString(", ") { "arg$it" }
                        appendLine("/** `$fullKey` (plural) */")
                        appendLine("public fun $funcName(count: kotlin.Int, $params): kotlin.String = io.github.octaviusframework.i18n.core.OctaviusI18n.lookupPlural(___data, io.github.octaviusframework.i18n.core.OctaviusI18n.currentLanguage, \"$fullKey\", count, $args)")
                        appendLine("/** `$fullKey` (plural - fraction) */")
                        appendLine("public fun $funcName(count: kotlin.Double, $params): kotlin.String = io.github.octaviusframework.i18n.core.OctaviusI18n.lookupPlural(___data, io.github.octaviusframework.i18n.core.OctaviusI18n.currentLanguage, \"$fullKey\", count, $args)")
                    } else {
                        appendLine("/** `$fullKey` (plural) */")
                        appendLine("public fun $funcName(count: kotlin.Int): kotlin.String = io.github.octaviusframework.i18n.core.OctaviusI18n.lookupPlural(___data, io.github.octaviusframework.i18n.core.OctaviusI18n.currentLanguage, \"$fullKey\", count)")
                        appendLine("/** `$fullKey` (plural - fraction) */")
                        appendLine("public fun $funcName(count: kotlin.Double): kotlin.String = io.github.octaviusframework.i18n.core.OctaviusI18n.lookupPlural(___data, io.github.octaviusframework.i18n.core.OctaviusI18n.currentLanguage, \"$fullKey\", count)")
                    }
                    appendLine()
                }

                is TranslationEntry.Nested -> {
                    val objectName = escapeName(toPascalCase(key))
                    appendLine("public object $objectName {")
                    indentLevel++
                    generateEntries(entry.children, fullKey)
                    indentLevel--
                    appendLine("}")
                    appendLine()
                }
            }
        }
    }
}