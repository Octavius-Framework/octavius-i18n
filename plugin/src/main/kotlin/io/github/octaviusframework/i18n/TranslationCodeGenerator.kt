package io.github.octaviusframework.i18n

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Parses a flat map of JSON elements into a tree of [TranslationEntry] values, detecting
 * parameterized templates (`{0}`, `{1}`, ...), plural form groups, and nested objects along the way.
 */
private fun parseTranslationMap(map: Map<String, JsonElement>): Map<String, TranslationEntry> {
    val result = mutableMapOf<String, TranslationEntry>()

    for ((key, element) in map) {
        result[key] = when (element) {
            is JsonPrimitive -> {
                val value = element.content
                val params = PARAM_REGEX.findAll(value).map { it.groupValues[1].toInt() }.toList()
                if (params.isNotEmpty()) {
                    TranslationEntry.Parameterized(value, params.max() + 1)
                } else {
                    TranslationEntry.Simple(value)
                }
            }
            is JsonObject -> {
                // Check if it's a plural form
                if (element.keys.all { it in PLURAL_KEYS } && element.values.all { it is JsonPrimitive && it.isString }) {
                    val pluralForms = element.mapValues { (it.value as JsonPrimitive).content }
                    TranslationEntry.Plural(pluralForms)
                } else {
                    TranslationEntry.Nested(parseTranslationMap(element))
                }
            }
            else -> TranslationEntry.Simple(element.toString())
        }
    }

    return result
}

/**
 * Flattens the translation tree into flat maps.
 */
private fun flattenTranslations(
    entries: Map<String, TranslationEntry>,
    prefix: String = ""
): Pair<Map<String, String>, Map<String, Map<String, String>>> {
    val simple = mutableMapOf<String, String>()
    val plural = mutableMapOf<String, Map<String, String>>()

    for ((key, entry) in entries) {
        val fullKey = if (prefix.isEmpty()) key else "$prefix.$key"

        when (entry) {
            is TranslationEntry.Simple -> simple[fullKey] = entry.value
            is TranslationEntry.Parameterized -> simple[fullKey] = entry.value
            is TranslationEntry.Plural -> plural[fullKey] = entry.forms
            is TranslationEntry.Nested -> {
                val (childSimple, childPlural) = flattenTranslations(entry.children, fullKey)
                simple.putAll(childSimple)
                plural.putAll(childPlural)
            }
        }
    }

    return simple to plural
}

/**
 * Escapes a string for use in Kotlin code.
 */
private fun escapeString(s: String): String {
    return s.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
        .replace("$", "\\$")
}

/**
 * Generates a file with translation data for a specific language.
 */
private class LanguageDataGenerator(private val packageName: String, private val objectName: String) {

    /**
     * Renders the `{objectName}Translations{Lang}.kt` source file contents for a single language,
     * embedding its flattened simple and plural translation maps as a [io.github.octaviusframework.i18n.core.TranslationData] object.
     */
    fun generate(lang: String, simple: Map<String, String>, plural: Map<String, Map<String, String>>): String {
        val langPascal = toPascalCase(lang)
        val className = "${objectName}Translations$langPascal"
        val builder = StringBuilder()

        builder.appendLine("@file:Suppress(\"unused\", \"RedundantVisibilityModifier\")")
        builder.appendLine()
        builder.appendLine("package $packageName")
        builder.appendLine()
        builder.appendLine("/**")
        builder.appendLine(" * Translation data for language: $lang")
        builder.appendLine(" *")
        builder.appendLine(" * This file is auto-generated. Do not edit manually.")
        builder.appendLine(" */")
        builder.appendLine("public object $className : io.github.octaviusframework.i18n.core.TranslationData {")
        builder.appendLine()

        // Simple translations
        builder.appendLine("    override val simple: kotlin.collections.Map<kotlin.String, kotlin.String> = mapOf(")
        simple.entries.sortedBy { it.key }.forEachIndexed { index, (key, value) ->
            val comma = if (index < simple.size - 1) "," else ""
            builder.appendLine("        \"$key\" to \"${escapeString(value)}\"$comma")
        }
        builder.appendLine("    )")
        builder.appendLine()

        // Plural translations
        builder.appendLine("    override val plural: kotlin.collections.Map<kotlin.String, io.github.octaviusframework.i18n.core.PluralForms> = mapOf(")
        plural.entries.sortedBy { it.key }.forEachIndexed { index, (key, forms) ->
            val comma = if (index < plural.size - 1) "," else ""
            val zero = forms["_zero"]?.let { "\"${escapeString(it)}\"" } ?: "null"
            val one = forms["_one"]?.let { "\"${escapeString(it)}\"" } ?: "null"
            val two = forms["_two"]?.let { "\"${escapeString(it)}\"" } ?: "null"
            val few = forms["_few"]?.let { "\"${escapeString(it)}\"" } ?: "null"
            val many = forms["_many"]?.let { "\"${escapeString(it)}\"" } ?: "null"
            val other = forms["_other"]?.let { "\"${escapeString(it)}\"" } ?: "\"$key\""
            builder.appendLine("        \"$key\" to io.github.octaviusframework.i18n.core.PluralForms($zero, $one, $two, $few, $many, $other)$comma")
        }
        builder.appendLine("    )")
        builder.appendLine("}")

        return builder.toString()
    }
}


/**
 * Gradle task that generates type-safe Kotlin classes from JSON translation files.
 *
 * Generated files:
 * - `{ObjectName}Translations{Lang}.kt` - flat maps with translations per language
 * - `{ObjectName}.kt` - registry pattern + type-safe accessors
 *
 * Example usage of generated code:
 * ```kotlin
 * Tr.Action.save()              // instead of T.get("action.save")
 * Tr.Form.Actions.itemLabel(1)  // instead of T.get("form.actions.itemLabel", 1)
 * Tr.Games.Form.category(5)     // instead of T.getPlural("games.form.category", 5)
 *
 * // Runtime language switching
 * Tr.currentLanguage = "en"
 * ```
 */
@CacheableTask
abstract class GenerateI18nTask : DefaultTask() {

    /** Target package where the generated code should be placed. */
    @get:Input
    abstract val targetPackage: Property<String>

    /** The name of the generated Kotlin object holding the translations. */
    @get:Input
    abstract val objectName: Property<String>

    /** The translation JSON source files to read and merge, one set per language. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: Property<FileCollection>

    /**
     * Directory where the generated Kotlin sources are written. Owned exclusively by this task:
     * it is wiped before every run, so renamed objects or removed languages leave no stale files behind.
     */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    /** The fallback language code used at runtime when a translation is missing in the current language. */
    @get:Input
    abstract val fallbackLanguage: Property<String>

    /**
     * Reads and merges all [sourceFiles] JSON translations by language, then writes the generated
     * per-language data classes and the type-safe accessor object into [outputDir].
     */
    @TaskAction
    fun generate() {
        val jsonParser = Json { ignoreUnknownKeys = true }
        val mergedByLang = mutableMapOf<String, MutableMap<String, JsonElement>>()
        
        val actualTargetPackage = targetPackage.get()
        val actualObjectName = objectName.get()

        sourceFiles.get().files.forEach { file ->
            if (file.isFile && file.name.endsWith(".json")) {
                val lang = file.name.substringBefore(".json")
                val content = file.readText(Charsets.UTF_8)
                if (content.isNotBlank()) {
                    logger.info("Found translation for '$lang' in ${file.path}")
                    try {
                        val sourceElement = jsonParser.parseToJsonElement(content)
                        if (sourceElement is JsonObject) {
                            val targetMap = mergedByLang.getOrPut(lang) { mutableMapOf() }
                            mergeJsonElements(targetMap, sourceElement)
                        }
                    } catch (e: Exception) {
                        logger.error("Failed to parse translation file: ${file.path}", e)
                    }
                }
            }
        }

        val outputDirFile = outputDir.get().asFile
        outputDirFile.deleteRecursively()
        outputDirFile.mkdirs()

        if (mergedByLang.isEmpty()) {
            logger.warn("No translation files found!")
            return
        }

        val packagePath = actualTargetPackage.replace(".", "/")

        // Generate files for each language
        for ((lang, translationMap) in mergedByLang) {
            logger.lifecycle("Generating translations for language: $lang")

            val entries = parseTranslationMap(translationMap)
            val (simpleMap, pluralMap) = flattenTranslations(entries)

            // Generate {ObjectName}Translations{Lang}.kt
            val langGenerator = LanguageDataGenerator(actualTargetPackage, actualObjectName)
            val langCode = langGenerator.generate(lang, simpleMap, pluralMap)
            val langFile = File(outputDirFile, "$packagePath/${actualObjectName}Translations${toPascalCase(lang)}.kt")
            langFile.parentFile.mkdirs()
            langFile.writeText(langCode, StandardCharsets.UTF_8)
            logger.lifecycle("Generated: ${langFile.path}")
        }

        // Generate Tr.kt (we use the first language as default language for runtime)
        val defaultLang = fallbackLanguage.get()
        val allLangs = mergedByLang.keys.toList()

        // Build a union of all translation maps to ensure ALL keys are generated
        val unionMap = mutableMapOf<String, JsonElement>()
        mergedByLang.values.forEach { langMap ->
            mergeJsonElements(unionMap, JsonObject(langMap))
        }
        val entries = parseTranslationMap(unionMap)

        val trGenerator = TrGenerator(actualTargetPackage, actualObjectName)
        val trCode = trGenerator.generate(entries, defaultLang, allLangs)
        val trFile = File(outputDirFile, "$packagePath/$actualObjectName.kt")
        trFile.writeText(trCode, StandardCharsets.UTF_8)
        logger.lifecycle("Generated: ${trFile.path}")

        // Statistics
        fun countFunctions(entries: Map<String, TranslationEntry>): Int {
            return entries.values.sumOf { entry ->
                when (entry) {
                    is TranslationEntry.Nested -> countFunctions(entry.children)
                    else -> 1
                }
            }
        }
        val functionCount = countFunctions(entries)
        val (simpleCount, pluralCount) = flattenTranslations(entries)
        logger.lifecycle("Generated $functionCount accessors (${simpleCount.size} simple, ${pluralCount.size} plural)")
    }
}
