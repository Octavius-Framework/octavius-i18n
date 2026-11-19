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

    @Test
    fun `a language's object is reached only through the branch of its code`() {
        val source = TrGenerator("org.example", "Tr").generate(translations, "en")
        assertContains(source, "private fun ___language(code: kotlin.String): TrTranslations? = when (code) {\n")
        assertContains(source, "    \"pl\" -> TrTranslationsPl\n")
        assertContains(source, "    \"en\" -> TrTranslationsEn\n")
    }

    private val nested = readTranslations(listOf(file("main/pl.json", """{ "Legion": { "Orders": { "march": "Marsz!" } } }""")))

    @Test
    fun `a group is documented on its property, by its whole path`() {
        val source = TrGenerator("org.example", "Tr").generate(nested, "pl")
        assertContains(source, "    /** The keys under `Legion`. */\n    public val Legion: Legion_Group\n")
        assertContains(source, "        /** The keys under `Legion.Orders`. */\n        public val Orders: Orders_Group\n")
    }

    @Test
    fun `a group is made the first time it is called`() {
        val source = TrGenerator("org.example", "Tr").generate(nested, "pl")
        assertContains(source, "public val Legion: Legion_Group\n        get() = ___Legion ?: Legion_Group().also { ___Legion = it }\n")
        assertContains(source, "    @kotlin.concurrent.Volatile\n    private var ___Legion: Legion_Group? = null\n")
    }

    @Test
    fun `the current language, as last found, is published through a volatile field`() {
        val source = TrGenerator("org.example", "Tr").generate(translations, "en")
        assertContains(source, "@kotlin.concurrent.Volatile\nprivate var ___found: kotlin.Pair<kotlin.String, TrTranslations>? = null\n")
    }
}
