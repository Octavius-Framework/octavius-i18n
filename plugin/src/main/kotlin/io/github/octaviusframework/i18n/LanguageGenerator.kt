package io.github.octaviusframework.i18n

/**
 * The source of `{objectName}Translations{Lang}.kt`: the translations of [keys] in [lang] compiled to Kotlin, one
 * function per key, implementing the translations interface, and the language's plural [rule] when a key asks it.
 */
internal fun languageSource(packageName: String, objectName: String, lang: String, keys: List<TranslationKey>, rule: PluralRule?): String {
    val className = "${objectName}Translations${toPascalCase(lang)}"
    val ruleForms = rule?.forms ?: setOf("other")
    return buildString {
        appendLine("package $packageName")
        appendLine()
        appendLine("/**")
        appendLine(" * The translations of [$objectName] in `$lang`.")
        appendLine(" *")
        appendLine(" * This file is auto-generated. Do not edit manually.")
        appendLine(" */")
        appendLine("internal object $className : ${objectName}Translations {")
        for (key in keys) {
            val message = key.messages.getValue(lang)
            appendLine()
            appendLine(
                "    override fun ${memberName(key.path)}(${key.signature.declaration()}): kotlin.String = " +
                    message.toKotlin(key.signature.names(), ruleForms, "    ")
            )
        }
        appendLine("}")
        if (keys.any { it.messages.getValue(lang).usesPluralRule(ruleForms) }) {
            appendLine()
            append(rule!!.functionSource())
        }
    }
}
