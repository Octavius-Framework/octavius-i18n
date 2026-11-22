package io.github.octaviusframework.i18n

import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SchemaRegistryConfig
import com.networknt.schema.SpecificationVersion
import com.networknt.schema.dialect.Dialects
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The JSON Schema of a translation file against the build. The schema checks one file at a time and leaves names,
 * placeholders and the languages to the build, so the files here are ones whose shape alone decides.
 */
class TranslationSchemaTest {

    private val schemaText = File(System.getProperty("translationSchema")).readText()

    // Errors in English, as the build's problems are, whatever the machine's language
    private val registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_7) {
        it.schemaRegistryConfig(SchemaRegistryConfig.builder().locale(Locale.ENGLISH).build())
    }
    private val schema = registry.getSchema(schemaText, InputFormat.JSON)

    /** The schema's errors in [json]. */
    private fun errors(json: String): List<String> = schema.validate(json, InputFormat.JSON).map { it.toString() }

    /** The build's problems with [json] as the only file, `en.json`. */
    private fun problems(json: String): List<String> =
        readTranslations(listOf(TranslationFile("en.json", "en", Json.parseToJsonElement(json).jsonObject))).problems

    /** Each of [files] the build or the schema does not judge as [accepted], with what each of them said. */
    private fun misjudged(files: List<String>, accepted: Boolean): List<String> = files.mapNotNull { json ->
        val build = problems(json)
        val schema = errors(json)
        if (build.isEmpty() == accepted && schema.isEmpty() == accepted) return@mapNotNull null
        "${json.trimIndent()}\n  build: ${build.ifEmpty { "accepts" }}\n  schema: ${schema.ifEmpty { "accepts" }}"
    }

    @Test
    fun `the schema is a draft-07 schema`() {
        val meta = registry.getSchema(SchemaLocation.of(Dialects.getDraft7().id))
        assertEquals(emptyList(), meta.validate(schemaText, InputFormat.JSON).map { it.toString() })
    }

    @Test
    fun `the schema accepts every translation file of the example`() {
        val files = File(System.getProperty("example")).walkTopDown()
            .onEnter { it.name != "build" && !it.name.startsWith('.') }
            .filter { it.isFile && it.extension == "json" && it.parentFile.name == "i18n" }
            .sorted()
            .toList()
        assertTrue(files.isNotEmpty())
        assertEquals(emptyMap(), files.associate { it.path to errors(it.readText()) }.filterValues { it.isNotEmpty() })
    }

    @Test
    fun `the schema accepts what the build accepts`() {
        val files = listOf(
            "{}",
            $$"""{ "$schema": "https://example.com/translation-file.schema.json", "march": "March!" }""",
            """{ "Legion": { "Orders": { "march": "March!" }, "Camp": {} } }""",
            $$"""
            {
              "cohort": {
                "$plural": "count",
                "=0": "no cohort", "=-1.5": "{count} cohorts",
                "zero": "{count} cohorts", "one": "{count} cohort", "two": "{count} cohorts",
                "few": "{count} cohorts", "many": "{count} cohorts", "other": "{count} cohorts"
              }
            }
            """,
            $$"""{ "place": { "$ordinal": "place", "=0": "last", "one": "{place}st", "two": "{place}nd", "few": "{place}rd", "other": "{place}th" } }""",
            $$"""
            {
              "invited": {
                "$select": "gender",
                "female": { "$plural": "count", "one": "{name} invited {count} friend", "other": "{name} invited {count} friends" },
                "other": { "$select": "rank", "=0": "{name} invited a recruit", "other": "{name} invited them" }
              }
            }
            """,
        )
        assertEquals(emptyList(), misjudged(files, accepted = true))
    }

    @Test
    fun `the schema rejects what the build rejects`() {
        val files = listOf(
            """{ "founded": 753 }""",
            """{ "victorious": true }""",
            """{ "triumph": null }""",
            """{ "ranks": ["hastati", "principes", "triarii"] }""",
            $$"""{ "cohort": { "$plural": "count", "one": null, "other": "{count} cohorts" } }""",
            $$"""{ "cohort": { "$plural": "count", "one": "{count} cohort" } }""",
            $$"""{ "place": { "$ordinal": "place", "one": "{place}st" } }""",
            $$"""{ "joined": { "$select": "gender", "female": "{name} joined" } }""",
            $$"""{ "cohort": { "$plural": "count", "fewer": "x", "other": "y" } }""",
            $$"""{ "cohort": { "$plural": "count", "=+1": "x", "other": "y" } }""",
            $$"""{ "cohort": { "$plural": "count", "$select": "gender", "other": "y" } }""",
            $$"""{ "cohort": { "$plural": "count", "$ordinal": "count", "other": "y" } }""",
            $$"""{ "place": { "$ordinal": "place", "fewer": "x", "other": "y" } }""",
            $$"""{ "place": { "$ordinal": "place", "$select": "gender", "other": "y" } }""",
            $$"""{ "joined": { "$choose": "gender", "other": "y" } }""",
            $$"""{ "joined": { "$select": "0", "other": "y" } }""",
            $$"""{ "cohort": { "$plural": 1, "other": "y" } }""",
            $$"""{ "joined": { "$select": "gender", "$rank": "x", "other": "y" } }""",
            $$"""{ "joined": { "$select": "gender", "female": { "text": "x" }, "other": "y" } }""",
            $$"""{ "joined": { "$select": "gender", "other": {} } }""",
            $$"""{ "$plural": "count", "other": "y" }""",
            $$"""{ "Legion": { "$schema": "https://example.com/translation-file.schema.json", "march": "March!" } }""",
        )
        assertEquals(emptyList(), misjudged(files, accepted = false))
    }
}
