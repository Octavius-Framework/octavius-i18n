package io.github.octaviusframework.i18n

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadTranslationsTest {

    private fun file(path: String, json: String) =
        TranslationFile(path, path.substringAfterLast('/').substringBefore(".json"), Json.parseToJsonElement(json).jsonObject)

    private fun problems(vararg files: TranslationFile) = readTranslations(files.toList()).problems

    /** The accessor's parameters for [key], as `name: Type`. */
    private fun signature(key: String, vararg files: TranslationFile): String {
        val translations = readTranslations(files.toList())
        assertEquals(emptyList(), translations.problems)
        return translations.keys.single { it.path == key }.signature.declaration()
    }

    @Test
    fun `files with the same keys in the same shape agree`() {
        val problems = problems(
            file("main/en.json", """{ "Legion": { "march": "March!", "greeting": "Ave, {name}! Legion {legion}." } }"""),
            file("main/pl.json", """{ "Legion": { "march": "Marsz!", "greeting": "Legion {legion}. Ave, {name}!" } }"""),
        )
        assertEquals(emptyList(), problems)
    }

    @Test
    fun `keys of one language may be split across files`() {
        val problems = problems(
            file("main/en.json", """{ "Legion": { "Orders": { "march": "March!" } } }"""),
            file("camp/en.json", """{ "Legion": { "Camp": { "tower": "Tower" } } }"""),
            file("main/pl.json", """{ "Legion": { "Orders": { "march": "Marsz!" }, "Camp": { "tower": "Wieża" } } }"""),
        )
        assertEquals(emptyList(), problems)
    }

    @Test
    fun `a key missing from one language is reported for that language`() {
        val problems = problems(
            file("main/en.json", """{ "Legion": { "march": "March!" } }"""),
            file("main/pl.json", """{ "Legion": { "march": "Marsz!", "retreat": "Odwrót!" } }"""),
        )
        assertEquals(listOf("en has no `Legion.retreat`"), problems)
    }

    @Test
    fun `a language with a file that could not be read is not reported for the keys it lacks`() {
        val problems = readTranslations(
            listOf(
                file("main/en.json", """{ "Legion": { "march": "March!", "retreat": "Retreat!" } }"""),
                file("main/pl.json", """{ "Legion": { "march": "Marsz!" } }"""),
            ),
            unreadable = setOf("pl"),
        ).problems
        assertEquals(emptyList(), problems)
    }

    @Test
    fun `a language CLDR has no rule for is reported when its plurals have forms to choose from`() {
        val problems = problems(
            file("main/polish.json", """{ "cohort": { "${'$'}plural": "count", "one": "{count} kohorta", "other": "{count} kohorty" } }"""),
        )
        assertEquals(
            listOf(
                "polish has no plural rule in CLDR ${CldrPlurals.version}, and its plurals have forms to choose from; " +
                    "a file is named after a language code, such as pl or pt-BR",
            ),
            problems,
        )
    }

    @Test
    fun `a language CLDR has no rule for needs none when its plurals have only exact values and other`() {
        val translations = readTranslations(
            listOf(file("main/polish.json", """{ "cohort": { "${'$'}plural": "count", "=0": "brak kohort", "other": "{count} kohort" } }""")),
        )
        assertEquals(emptyList(), translations.problems)
        assertEquals(emptyMap(), translations.rules)
    }

    @Test
    fun `a key defined in two files of one language names both files`() {
        val problems = problems(
            file("main/pl.json", """{ "Legion": { "march": "Marsz!" } }"""),
            file("camp/pl.json", """{ "Legion": { "march": "Naprzód!" } }"""),
        )
        assertEquals(listOf("`Legion.march` is defined in both main/pl.json and camp/pl.json"), problems)
    }

    @Test
    fun `forms of one plural split across two files are a duplicate`() {
        val problems = problems(
            file("main/pl.json", """{ "cohort": { "${'$'}plural": "count", "one": "{count} kohorta", "other": "{count} kohorty" } }"""),
            file("camp/pl.json", """{ "cohort": { "${'$'}plural": "count", "many": "{count} kohort" } }"""),
        )
        assertEquals(listOf("`cohort` is defined in both main/pl.json and camp/pl.json"), problems)
    }

    @Test
    fun `a key that is a translation in one file and a group in another is reported`() {
        val problems = problems(
            file("main/pl.json", """{ "Legion": { "Orders": "Rozkazy" } }"""),
            file("camp/pl.json", """{ "Legion": { "Orders": { "march": "Marsz!" } } }"""),
        )
        assertEquals(listOf("`Legion.Orders` is a translation in main/pl.json and a group of keys in camp/pl.json"), problems)
    }

    @Test
    fun `an empty object is an empty group`() {
        val translations = readTranslations(listOf(file("main/pl.json", """{ "Legion": {} }""")))
        assertEquals(emptyList(), translations.problems)
        assertEquals(emptyList(), translations.keys)
    }

    @Test
    fun `an object without a dollar key is a group, whatever its keys are called`() {
        val translations = readTranslations(listOf(file("main/pl.json", """{ "cohort": { "_one": "kohorta", "_other": "kohorty" } }""")))
        assertEquals(emptyList(), translations.problems)
        assertEquals(listOf("cohort._one", "cohort._other"), translations.keys.map { it.path })
    }

    @Test
    fun `an argument inserted into the text in one language must be in every language`() {
        val problems = problems(
            file("main/en.json", """{ "greeting": "Ave, {name}! Welcome to the {legion} legion." }"""),
            file("main/uk.json", """{ "greeting": "Ave, {name}! Легіон {legion}." }"""),
            file("main/pl.json", """{ "greeting": "Ave, {name}!" }"""),
        )
        assertEquals(listOf("`greeting` uses {legion} in en, uk but not in pl"), problems)
    }

    @Test
    fun `plurals may have different forms, and leave out the count, but not their other arguments`() {
        val problems = problems(
            file(
                "main/en.json",
                """{ "cohort": { "${'$'}plural": "count", "one": "one cohort", "other": "{count} cohorts" }, "built": { "${'$'}plural": "count", "other": "{count} built {what}" } }""",
            ),
            file(
                "main/pl.json",
                """{ "cohort": { "${'$'}plural": "count", "one": "{count} kohorta", "few": "{count} kohorty", "many": "{count} kohort", "other": "{count} kohorty" }, "built": { "${'$'}plural": "count", "other": "{count} zbudowało" } }""",
            ),
        )
        assertEquals(listOf("`built` uses {what} in en but not in pl"), problems)
    }

    @Test
    fun `a language may leave out an argument it does not choose by`() {
        val signature = signature(
            "arrived",
            file("main/en.json", """{ "arrived": "{name} arrived in Rome" }"""),
            file("main/pl.json", """{ "arrived": { "${'$'}select": "gender", "female": "{name} przybyła do Rzymu", "other": "{name} przybył do Rzymu" } }"""),
        )
        assertEquals("gender: kotlin.String, name: kotlin.Any", signature)
    }

    @Test
    fun `a language may count with a plural where another has a plain string`() {
        val signature = signature(
            "cohort",
            file("main/ja.json", """{ "cohort": "{count} コホート" }"""),
            file("main/pl.json", """{ "cohort": { "${'$'}plural": "count", "one": "{count} kohorta", "other": "{count} kohorty" } }"""),
        )
        assertEquals("count: kotlin.Number", signature)
    }

    @Test
    fun `arguments that choose come first, then the others, each alphabetically`() {
        val signature = signature(
            "report",
            file(
                "main/en.json",
                """{ "report": { "${'$'}select": "role", "other": { "${'$'}plural": "count", "other": "{zeta} {alpha} {count}" } } }""",
            ),
        )
        assertEquals("count: kotlin.Number, role: kotlin.String, alpha: kotlin.Any, zeta: kotlin.Any", signature)
    }

    @Test
    fun `a keyword is a parameter in backticks`() {
        assertEquals("`in`: kotlin.Any", signature("gate", file("main/en.json", """{ "gate": "Gate {in}" }""")))
    }

    @Test
    fun `a number in braces is reported`() {
        val problems = problems(
            file("main/pl.json", """{ "pair": "{0} i {1}", "one": { "${'$'}select": "gender", "other": "{0}" } }"""),
        )
        assertEquals(
            listOf(
                "`pair` in main/pl.json: `{0}`, `{1}` are not placeholders; a placeholder is a name, such as `{count}`",
                "`one` in main/pl.json, under `other`: `{0}` is not a placeholder; a placeholder is a name, such as `{count}`",
            ),
            problems,
        )
    }

    @Test
    fun `braces around anything else are text`() {
        assertEquals("", signature("text", file("main/pl.json", """{ "text": "{ name } {_x} {1st} {}" }""")))
    }

    @Test
    fun `a choice without other is reported where it is`() {
        val problems = problems(
            file(
                "main/pl.json",
                """
                {
                  "joined": { "${'$'}select": "gender", "female": "{name} dołączyła" },
                  "invited": { "${'$'}select": "gender", "female": { "${'$'}plural": "count", "one": "{name} zaprosiła {count}" }, "other": "{name} zaprosił" }
                }
                """,
            ),
        )
        assertEquals(
            listOf(
                "`joined` in main/pl.json: the `${'$'}select` has no `other`",
                "`invited` in main/pl.json, under `female`: the `${'$'}plural` has no `other`",
            ),
            problems,
        )
    }

    @Test
    fun `objects that are not choices are reported`() {
        val problems = problems(
            file(
                "main/pl.json",
                """
                {
                  "a": { "${'$'}plural": "count", "fewer": "x", "other": "y" },
                  "b": { "${'$'}plural": "count", "${'$'}select": "gender", "other": "y" },
                  "c": { "${'$'}choose": "gender", "other": "y" },
                  "d": { "${'$'}select": "0", "other": "y" },
                  "e": { "${'$'}select": "gender", "female": { "text": "x" }, "other": "y" }
                }
                """,
            ),
        )
        assertEquals(
            listOf(
                "`a` in main/pl.json: `fewer` is not a form; a `${'$'}plural` takes zero, one, two, few, many, other and exact values such as `=0`",
                "`b` in main/pl.json: `${'$'}plural` and `${'$'}select` are in one object; put one in the branches of the other",
                "`c` in main/pl.json: `${'$'}choose` is not `${'$'}plural` or `${'$'}select`",
                "`d` in main/pl.json: `${'$'}select` names the argument it chooses by, such as \"count\" or \"gender\"",
                "`e` in main/pl.json, under `female`: a branch is a string, or an object with `${'$'}plural` or `${'$'}select`",
            ),
            problems,
        )
    }

    @Test
    fun `an argument that chooses a plural form in one language and a case in another is reported`() {
        val problems = problems(
            file("main/en.json", """{ "joined": { "${'$'}plural": "who", "other": "joined" } }"""),
            file("main/pl.json", """{ "joined": { "${'$'}select": "who", "other": "dołączył" } }"""),
        )
        assertEquals(listOf("`joined` chooses by `who` as a `${'$'}plural` in en and as a `${'$'}select` in pl"), problems)
    }
}
