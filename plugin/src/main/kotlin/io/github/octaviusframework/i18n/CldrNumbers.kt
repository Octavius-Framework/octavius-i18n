package io.github.octaviusframework.i18n

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * CLDR's number formats, from `cldr/numbers.json`, which the `cldrNumberFormats` task of the plugin's build
 * extracts from CLDR.
 */
internal object CldrNumbers {

    /** The CLDR release the formats come from. */
    val version: String

    /** Each locale's format, by its code in lower case. */
    private val formats: Map<String, CldrNumberFormat>

    init {
        val text = CldrNumbers::class.java.getResource("cldr/numbers.json")!!.readText(Charsets.UTF_8)
        val data = Json.parseToJsonElement(text).jsonObject
        version = data.getValue("cldrVersion").jsonPrimitive.content
        val digits = data.getValue("digits").jsonObject.mapValues { (_, list) -> list.jsonArray.map { it.jsonPrimitive.content } }
        val distinct = data.getValue("formats").jsonArray.map { element ->
            val format = element.jsonObject
            fun text(name: String) = format.getValue(name).jsonPrimitive.content
            fun number(name: String) = format.getValue(name).jsonPrimitive.int
            CldrNumberFormat(
                text("decimal"), text("group"), text("minus"),
                number("minimumGrouping"), number("primaryGrouping"), number("secondaryGrouping"),
                digits.getValue(text("digits")),
            )
        }
        formats = data.getValue("locales").jsonObject.mapValues { (_, index) -> distinct[index.jsonPrimitive.int] }
    }

    /**
     * The format for [language], a file's language code: the code itself, or with its last part cut off until CLDR
     * has it, as `pt-BR` takes `pt`; CLDR's root format when it has none of them.
     */
    fun formatFor(language: String): CldrNumberFormat {
        var code = languageCode(language)
        while (true) {
            formats[code]?.let { return it }
            val cut = code.lastIndexOf('-')
            if (cut < 0) return formats.getValue("und")
            code = code.substring(0, cut)
        }
    }
}
