package io.github.octaviusframework.i18n.core

/**
 * Main utility object for Octavius I18n plugin.
 * Handles current language state, plural rules, and formatting translations.
 */
object OctaviusI18n {
    
    /**
     * The currently selected language code (e.g. "en", "pl").
     * Change this property to switch the application's locale at runtime.
     */
    var currentLanguage: String = "en"
    
    /**
     * Map of available plural rules by language code.
     * Contains built-in rules for English and Polish by default.
     */
    val pluralRules: MutableMap<String, PluralRule> = mutableMapOf(
        "en" to object : PluralRule {
            override fun selectForm(count: Int) = if (count == 1) "one" else "other"
            override fun selectForm(count: Double) = "other"
        },
        "pl" to object : PluralRule {
            override fun selectForm(count: Int) = when {
                count == 1 -> "one"
                count % 10 in 2..4 && count % 100 !in 12..14 -> "few"
                else -> "many"
            }
            override fun selectForm(count: Double) = "other"
        }
    )

    /**
     * Formats a translation template by replacing `{0}`, `{1}`, etc., with the provided arguments.
     *
     * @param template The string template, e.g., "Hello {0}!"
     * @param args The arguments to inject into the template.
     * @return The formatted string.
     */
    public fun formatString(template: String, vararg args: Any): String {
        if (args.isEmpty()) return template
        var result = template
        args.forEachIndexed { index, arg ->
            result = result.replace("{$index}", arg.toString())
        }
        return result
    }

    /**
     * Resolves the appropriate plural form for a given language and integer count.
     * 
     * @param lang The language code (e.g. "en", "pl").
     * @param count The integer amount for which the plural form is evaluated.
     * @return The plural category string (e.g., "zero", "one", "two", "few", "many", "other").
     */
    fun selectPluralForm(lang: String, count: Int): String {
        val rule = pluralRules[lang] ?: pluralRules["en"] ?: object : PluralRule {
            override fun selectForm(count: Int) = "other"
            override fun selectForm(count: Double) = "other"
        }
        return rule.selectForm(count)
    }

    fun selectPluralForm(lang: String, count: Double): String {
        val rule = pluralRules[lang] ?: pluralRules["en"] ?: object : PluralRule {
            override fun selectForm(count: Int) = "other"
            override fun selectForm(count: Double) = "other"
        }
        return rule.selectForm(count)
    }

    /**
     * Retrieves a simple translation by key from the given data set and formats it with arguments.
     * 
     * @param data The TranslationData containing the map of strings.
     * @param key The key to look up in the simple translations map.
     * @param args Formatting arguments to be injected into the result.
     * @return The formatted translation or the key itself if not found.
     */
    fun lookup(data: TranslationData, key: String, vararg args: Any): String {
        val template = data.simple[key] ?: return key
        return formatString(template, *args)
    }

    /**
     * Retrieves a plural translation by key from the given data set, matching the correct plural form 
     * based on the given integer count, and formats it with arguments.
     * 
     * @param data The TranslationData containing the map of plurals.
     * @param lang The language code to evaluate plural rules.
     * @param key The key to look up in the plural translations map.
     * @param count The integer amount used for pluralization.
     * @param args Additional formatting arguments to be injected. The count is injected as the first argument automatically.
     * @return The formatted translation or the key itself if not found.
     */
    fun lookupPlural(data: TranslationData, lang: String, key: String, count: Int, vararg args: Any): String {
        val forms = data.plural[key] ?: return key
        if (count == 0) forms.zero?.let { return formatString(it, count, *args) }
        val formName = selectPluralForm(lang, count)
        val formTemplate = when (formName) {
            "zero" -> forms.zero
            "one" -> forms.one
            "two" -> forms.two
            "few" -> forms.few
            "many" -> forms.many
            else -> forms.other
        } ?: forms.other
        return formatString(formTemplate, count, *args)
    }

    fun lookupPlural(data: TranslationData, lang: String, key: String, count: Double, vararg args: Any): String {
        val forms = data.plural[key] ?: return key
        if (count == 0.0) forms.zero?.let { return formatString(it, count, *args) }
        val formName = selectPluralForm(lang, count)
        val formTemplate = when (formName) {
            "zero" -> forms.zero
            "one" -> forms.one
            "two" -> forms.two
            "few" -> forms.few
            "many" -> forms.many
            else -> forms.other
        } ?: forms.other
        return formatString(formTemplate, count, *args)
    }
}
