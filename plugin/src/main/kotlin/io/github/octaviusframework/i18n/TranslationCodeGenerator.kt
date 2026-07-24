package io.github.octaviusframework.i18n

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Generates type-safe Kotlin classes from JSON translation files.
 *
 * Generated files:
 * - `Translations{Lang}.kt` - flat maps with translations per language
 * - `Tr.kt` - registry pattern + type-safe accessors
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

/**
 * Parses a JSON map into a TranslationEntry tree.
 */
private fun parseTranslationMap(map: Map<String, Any?>): Map<String, TranslationEntry> {
    val result = mutableMapOf<String, TranslationEntry>()

    for ((key, value) in map) {
        result[key] = when (value) {
            is String -> {
                val params = PARAM_REGEX.findAll(value).map { it.groupValues[1].toInt() }.toList()
                if (params.isNotEmpty()) {
                    TranslationEntry.Parameterized(value, params.max() + 1)
                } else {
                    TranslationEntry.Simple(value)
                }
            }
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val childMap = value as Map<String, Any?>

                // Check if it's a plural form
                if (childMap.keys.all { it in PLURAL_KEYS } && childMap.values.all { it is String }) {
                    @Suppress("UNCHECKED_CAST")
                    TranslationEntry.Plural(childMap as Map<String, String>)
                } else {
                    TranslationEntry.Nested(parseTranslationMap(childMap))
                }
            }
            else -> TranslationEntry.Simple(value?.toString() ?: "")
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
private class LanguageDataGenerator(private val packageName: String) {

    fun generate(lang: String, simple: Map<String, String>, plural: Map<String, Map<String, String>>): String {
        val langPascal = toPascalCase(lang)
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
        builder.appendLine("public object Translations$langPascal : io.github.octaviusframework.i18n.core.TranslationData {")
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
 * Registers the `generateI18nAccessors` task in the root project.
 *
 * The task scans all modules for `i18n\*.json` files,
 * merges them in memory and generates:
 * - Translations{Lang}.kt - flat maps with data per language
 * - Tr.kt - registry + type-safe accessors
 *
 * @param coreProject Core project where the files will be generated
 * @param targetPackage Package for the generated code
 */
fun Project.registerGenerateI18nAccessorsTask(
    coreProject: Project,
    sourceProject: Project = rootProject,
    targetPackage: String = "org.octavius.localization",
    objectName: String = "Tr"
): TaskProvider<*> {
    return tasks.register("generateI18nAccessors") {
        group = "build"
        description = "Generates type-safe Kotlin accessors for translations."

        val outputDir = coreProject.layout.buildDirectory.dir("generated/kotlin/commonMain")
        outputs.dir(outputDir)

        // Gather all translation files as inputs
        sourceProject.allprojects.forEach { sub ->
            // Use fileTree to track only jsons
            inputs.files(sub.fileTree("src") { include("**/i18n/*.json") })
        }

        doLast {
            val gson = Gson()
            val mapType = object : TypeToken<Map<String, Any?>>() {}.type
            val mergedByLang = mutableMapOf<String, MutableMap<String, Any?>>()

            // Scan all subprojects
            sourceProject.allprojects.forEach { subproject ->
                subproject.file("src").walk().forEach { file ->
                    if (file.isFile && file.parentFile?.name == "i18n" && file.name.endsWith(".json")) {
                        val lang = file.name.substringBefore(".json")
                        val content = file.readText(Charsets.UTF_8)
                        if (content.isNotBlank()) {
                            logger.info("Found translation for '$lang' in ${subproject.name}/${file.relativeTo(subproject.projectDir)}")
                            try {
                                val sourceMap: Map<String, Any?> = gson.fromJson(content, mapType)
                                val targetMap = mergedByLang.getOrPut(lang) { mutableMapOf() }
                                mergeJsonMaps(targetMap, sourceMap)
                            } catch (e: Exception) {
                                logger.error("Failed to parse translation file: ${file.path}", e)
                            }
                        }
                    }
                }
            }

            if (mergedByLang.isEmpty()) {
                logger.warn("No translation files found!")
                return@doLast
            }

            val packagePath = targetPackage.replace(".", "/")
            val outputDirFile = outputDir.get().asFile

            // No longer generating TranslationData and PluralForms here as they are in the runtime library

            // Generate files for each language
            for ((lang, translationMap) in mergedByLang) {
                logger.lifecycle("Generating translations for language: $lang")

                val entries = parseTranslationMap(translationMap)
                val (simpleMap, pluralMap) = flattenTranslations(entries)

                // Generate Translations{Lang}.kt
                val langGenerator = LanguageDataGenerator(targetPackage)
                val langCode = langGenerator.generate(lang, simpleMap, pluralMap)
                val langFile = File(outputDirFile, "$packagePath/Translations${toPascalCase(lang)}.kt")
                langFile.parentFile.mkdirs()
                langFile.writeText(langCode, StandardCharsets.UTF_8)
                logger.lifecycle("Generated: ${langFile.path}")
            }

            // Generate Tr.kt (we use the first language as default)
            val (defaultLang, defaultMap) = mergedByLang.entries.first()
            val allLangs = mergedByLang.keys.toList()
            val entries = parseTranslationMap(defaultMap)

            val trGenerator = TrGenerator(targetPackage, objectName)
            val trCode = trGenerator.generate(entries, defaultLang, allLangs)
            val trFile = File(outputDirFile, "$packagePath/$objectName.kt")
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
}
