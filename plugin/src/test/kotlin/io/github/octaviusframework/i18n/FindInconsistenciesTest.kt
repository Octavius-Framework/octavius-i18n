package io.github.octaviusframework.i18n

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class FindInconsistenciesTest {

    private fun file(path: String, json: String) =
        TranslationFile(path, path.substringAfterLast('/').substringBefore(".json"), Json.parseToJsonElement(json).jsonObject)

    @Test
    fun `files with the same keys in the same shape agree`() {
        val problems = findInconsistencies(listOf(
            file("main/en.json", """{ "Legion": { "march": "March!", "greeting": "Ave, {0}! Legion {1}." } }"""),
            file("main/pl.json", """{ "Legion": { "march": "Marsz!", "greeting": "Legion {1}. Ave, {0}!" } }"""),
        ))
        assertEquals(emptyList(), problems)
    }

    @Test
    fun `keys of one language may be split across files`() {
        val problems = findInconsistencies(listOf(
            file("main/en.json", """{ "Legion": { "Orders": { "march": "March!" } } }"""),
            file("camp/en.json", """{ "Legion": { "Camp": { "tower": "Tower" } } }"""),
            file("main/pl.json", """{ "Legion": { "Orders": { "march": "Marsz!" }, "Camp": { "tower": "Wieża" } } }"""),
        ))
        assertEquals(emptyList(), problems)
    }

    @Test
    fun `a key missing from one language is reported for that language`() {
        val problems = findInconsistencies(listOf(
            file("main/en.json", """{ "Legion": { "march": "March!" } }"""),
            file("main/pl.json", """{ "Legion": { "march": "Marsz!", "retreat": "Odwrót!" } }"""),
        ))
        assertEquals(listOf("en has no `Legion.retreat`"), problems)
    }

    @Test
    fun `a key defined in two files of one language names both files`() {
        val problems = findInconsistencies(listOf(
            file("main/pl.json", """{ "Legion": { "march": "Marsz!" } }"""),
            file("camp/pl.json", """{ "Legion": { "march": "Naprzód!" } }"""),
        ))
        assertEquals(listOf("`Legion.march` is defined in both main/pl.json and camp/pl.json"), problems)
    }

    @Test
    fun `forms of one plural split across two files are a duplicate`() {
        val problems = findInconsistencies(listOf(
            file("main/pl.json", """{ "cohort": { "_one": "{0} kohorta" } }"""),
            file("camp/pl.json", """{ "cohort": { "_many": "{0} kohort" } }"""),
        ))
        assertEquals(listOf("`cohort` is defined in both main/pl.json and camp/pl.json"), problems)
    }

    @Test
    fun `a key that is a translation in one file and a group in another is reported`() {
        val problems = findInconsistencies(listOf(
            file("main/pl.json", """{ "Legion": { "Orders": "Rozkazy" } }"""),
            file("camp/pl.json", """{ "Legion": { "Orders": { "march": "Marsz!" } } }"""),
        ))
        assertEquals(listOf("`Legion.Orders` is a translation in main/pl.json and a group of keys in camp/pl.json"), problems)
    }

    @Test
    fun `a string in one language and a plural in another is reported`() {
        val problems = findInconsistencies(listOf(
            file("main/en.json", """{ "cohort": "cohort" }"""),
            file("main/pl.json", """{ "cohort": { "_one": "{0} kohorta", "_other": "{0} kohorty" } }"""),
        ))
        assertEquals(listOf("`cohort` is not the same in every language: a string with no placeholders in en; a plural in pl"), problems)
    }

    @Test
    fun `different placeholders are reported, and the languages that agree are grouped`() {
        val problems = findInconsistencies(listOf(
            file("main/en.json", """{ "greeting": "Ave, {0}! Welcome to the {1} legion." }"""),
            file("main/uk.json", """{ "greeting": "Ave, {0}! Легіон {1}." }"""),
            file("main/pl.json", """{ "greeting": "Ave, {0}!" }"""),
        ))
        assertEquals(listOf("`greeting` is not the same in every language: a string using {0}, {1} in en, uk; a string using {0} in pl"), problems)
    }

    @Test
    fun `plurals may have different forms, and leave out the count, but not their other placeholders`() {
        val problems = findInconsistencies(listOf(
            file("main/en.json", """{ "cohort": { "_one": "one cohort", "_other": "{0} cohorts" }, "built": { "_other": "{0} built {1}" } }"""),
            file("main/pl.json", """{ "cohort": { "_one": "{0} kohorta", "_few": "{0} kohorty", "_many": "{0} kohort", "_other": "{0} kohorty" }, "built": { "_other": "{0} zbudowało" } }"""),
        ))
        assertEquals(listOf("`built` is not the same in every language: a plural using {1} in en; a plural in pl"), problems)
    }
}
