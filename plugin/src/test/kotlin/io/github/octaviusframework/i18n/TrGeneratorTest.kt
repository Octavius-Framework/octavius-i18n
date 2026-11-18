package io.github.octaviusframework.i18n

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertContains

class TrGeneratorTest {

    private fun file(path: String, json: String) =
        TranslationFile(path, path.substringAfterLast('/').removeSuffix(".json"), Json.parseToJsonElement(json).jsonObject)

    private val translations = readTranslations(
        listOf(
            file("main/pl.json", """{ "greeting": "Ave, {name}!" }"""),
            file("main/en.json", """{ "greeting": "Hail, {name}!" }"""),
        ),
    )

    @Test
    fun `the fallback language is found as the current one is, and its templates document the accessors`() {
        for (fallback in listOf("en", "EN", "en-US", "en_GB")) {
            val source = TrGenerator("org.example", "Tr").generate(translations, fallback)
            assertContains(source, "/** `greeting` - Template: `Hail, {name}!` */", message = fallback)
            assertContains(source, "___lookup(language) ?: TrTranslationsEn\n", message = fallback)
        }
    }

    @Test
    fun `a fallback language with no file falls back to the first language`() {
        val source = TrGenerator("org.example", "Tr").generate(translations, "de")
        assertContains(source, "/** `greeting` - Template: `Ave, {name}!` */")
        assertContains(source, "___lookup(language) ?: TrTranslationsPl\n")
    }
}
