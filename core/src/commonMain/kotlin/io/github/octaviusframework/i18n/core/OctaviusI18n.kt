package io.github.octaviusframework.i18n.core

object OctaviusI18n {
    var currentLanguage: String = "en"
    
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

    public fun formatString(template: String, vararg args: Any): String {
        if (args.isEmpty()) return template
        var result = template
        args.forEachIndexed { index, arg ->
            result = result.replace("{$index}", arg.toString())
        }
        return result
    }

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

    fun lookup(data: TranslationData, key: String, vararg args: Any): String {
        val template = data.simple[key] ?: return key
        return formatString(template, *args)
    }

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
