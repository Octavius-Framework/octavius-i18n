package io.github.octaviusframework.i18n

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
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
 * Gradle task that generates type-safe Kotlin classes from JSON translation files.
 *
 * Generated files:
 * - `{ObjectName}Translations{Lang}.kt` - one language's translations compiled to functions
 * - `{ObjectName}.kt` - the accessor object and the interface every language implements
 *
 * The task fails, listing every problem at once, when the files cannot be generated from: a file
 * that is not a JSON object, or the problems [readTranslations] describes.
 *
 * Example usage of generated code:
 * ```kotlin
 * Tr.Legion.Orders.march()            // the string under "Legion.Orders.march"
 * Tr.Legion.greeting("IX", "Marcus")  // {legion} and {name} become parameters
 * Tr.Legion.Units.cohort(5)           // a plural key takes the count
 *
 * // Runtime language switching, for every generated object at once
 * OctaviusI18n.currentLanguage = "en"
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

    /** The language used at runtime when the current language has no translation file. */
    @get:Input
    abstract val fallbackLanguage: Property<String>

    /**
     * Reads all [sourceFiles] JSON translations, checks that they can be generated, then writes each language's
     * translations and the type-safe accessor object into [outputDir].
     */
    @TaskAction
    fun generate() {
        val translationFiles = mutableListOf<TranslationFile>()
        val unreadable = mutableSetOf<String>()
        val problems = mutableListOf<String>()

        val actualTargetPackage = targetPackage.get()
        val actualObjectName = objectName.get()

        sourceFiles.get().files.forEach { file ->
            if (file.isFile && file.name.endsWith(".json")) {
                val lang = file.name.substringBefore(".json")
                val content = file.readText(Charsets.UTF_8)
                if (content.isNotBlank()) {
                    logger.info("Found translation for '$lang' in ${file.path}")
                    try {
                        val sourceElement = Json.parseToJsonElement(content)
                        if (sourceElement is JsonObject) {
                            translationFiles += TranslationFile(file.path, lang, sourceElement)
                        } else {
                            problems += "${file.path} is not a JSON object"
                            unreadable += lang
                        }
                    } catch (e: SerializationException) {
                        problems += invalidJson(file.path, content, e)
                        unreadable += lang
                    }
                }
            }
        }

        // Every language must have every key, and every key must be a message, before any of it is generated
        val translations = readTranslations(translationFiles, unreadable)
        problems += translations.problems
        if (problems.isNotEmpty()) {
            throw GradleException(problems.joinToString("\n", prefix = "Problems in the translation files:\n") { "- $it" })
        }

        val outputDirFile = outputDir.get().asFile
        outputDirFile.deleteRecursively()
        outputDirFile.mkdirs()

        if (translations.languages.isEmpty()) {
            logger.warn("No translation files found!")
            return
        }

        val packageDir = File(outputDirFile, actualTargetPackage.replace(".", "/"))
        packageDir.mkdirs()

        for (lang in translations.languages) {
            logger.lifecycle("Generating translations for language: $lang")
            val langFile = File(packageDir, "${actualObjectName}Translations${toPascalCase(lang)}.kt")
            langFile.writeText(languageSource(actualTargetPackage, actualObjectName, lang, translations.keys), StandardCharsets.UTF_8)
            logger.lifecycle("Generated: ${langFile.path}")
        }

        val trFile = File(packageDir, "$actualObjectName.kt")
        trFile.writeText(TrGenerator(actualTargetPackage, actualObjectName).generate(translations, fallbackLanguage.get()), StandardCharsets.UTF_8)
        logger.lifecycle("Generated: ${trFile.path}")

        logger.lifecycle("Generated ${translations.keys.size} accessors")
    }
}

private val JSON_ERROR = Regex("""^Unexpected JSON token at offset (\d+): (.*?)(?: at path: \S*)?$""")

/**
 * The problem kotlinx.serialization found in the file at [path], as `path:line:column` and the reason, which the
 * IDE links to the place. Its message counts a character offset, and the lines after the first repeat the input
 * and advise on `Json {}` settings.
 */
internal fun invalidJson(path: String, content: String, e: SerializationException): String {
    val first = e.message?.lineSequence()?.first().orEmpty()
    val match = JSON_ERROR.find(first) ?: return "$path is not valid JSON: $first"
    val offset = match.groupValues[1].toInt().coerceAtMost(content.length)
    val line = content.take(offset).count { it == '\n' } + 1
    val column = offset - (content.lastIndexOf('\n', offset - 1) + 1) + 1
    return "$path:$line:$column is not valid JSON: ${match.groupValues[2]}"
}
