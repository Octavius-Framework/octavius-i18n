package io.github.octaviusframework.i18n

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** CLDR's plural rules, from the copy of `cldr-core/supplemental/plurals.json` the plugin carries. */
internal object CldrPlurals {

    /** The CLDR release the rules come from. */
    val version: String

    /** Each locale's entry, by its CLDR code: the text of every form's rule, samples included. */
    val entries: Map<String, Map<String, String>>

    /** Each locale's rule, by its CLDR code in lower case. */
    private val rules: Map<String, PluralRule>

    init {
        val text = CldrPlurals::class.java.getResource("cldr/plurals.json")!!.readText(Charsets.UTF_8)
        val supplemental = Json.parseToJsonElement(text).jsonObject.getValue("supplemental").jsonObject
        version = supplemental.getValue("version").jsonObject.getValue("_cldrVersion").jsonPrimitive.content
        entries = supplemental.getValue("plurals-type-cardinal").jsonObject.mapValues { (_, entry) ->
            (entry as JsonObject).mapValues { it.value.jsonPrimitive.content }
        }
        rules = entries.entries.associate { (locale, entry) -> locale.lowercase() to PluralRule.parse(locale, entry) }
    }

    /**
     * The rule for [language], a file's language code: the code itself, or with its last part cut off until CLDR
     * has it, as `pt-BR` takes `pt`. Case does not matter, and `_` separates parts as `-` does. Null when CLDR has
     * no rule for any of them.
     */
    fun ruleFor(language: String): PluralRule? {
        var code = language.lowercase().replace('_', '-')
        while (true) {
            rules[code]?.let { return it }
            val cut = code.lastIndexOf('-')
            if (cut < 0) return null
            code = code.substring(0, cut)
        }
    }
}
