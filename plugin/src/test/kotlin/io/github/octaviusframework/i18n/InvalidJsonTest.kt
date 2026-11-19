package io.github.octaviusframework.i18n

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InvalidJsonTest {

    private fun problem(content: String): String {
        val e = assertFailsWith<SerializationException> { Json.parseToJsonElement(content) }
        return invalidJson("main/pl.json", content, e)
    }

    @Test
    fun `the problem is placed at its line and column`() {
        assertEquals(
            "main/pl.json:3:13 is not valid JSON: Expected end of the object or comma",
            problem("{\n  \"a\": \"x\",\n  \"b\": \"y\" \"c\"\n}"),
        )
    }

    @Test
    fun `an empty file is reported as empty`() {
        val empty = "main/pl.json is empty; a translation file is a JSON object, `{}` while it has no keys"
        assertEquals(empty, problem(""))
        assertEquals(empty, problem("  \n "))
    }

    @Test
    fun `advice on Json settings is left out`() {
        assertEquals(
            "main/pl.json:1:12 is not valid JSON: Trailing comma before the end of JSON object",
            problem("{ \"a\": \"x\", }"),
        )
    }
}
