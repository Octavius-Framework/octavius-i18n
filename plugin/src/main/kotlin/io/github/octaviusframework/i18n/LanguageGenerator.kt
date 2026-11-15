package io.github.octaviusframework.i18n

/**
 * The source of `{objectName}Translations{Lang}.kt`: the translations of [keys] in [lang] compiled to Kotlin, one
 * function per key, implementing the translations interface.
 */
internal fun languageSource(packageName: String, objectName: String, lang: String, keys: List<TranslationKey>): String {
    val className = "${objectName}Translations${toPascalCase(lang)}"
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
                    message.toKotlin(key.signature.names(), "    ")
            )
        }
        appendLine("}")
        if (keys.any { it.messages.getValue(lang).usesPluralRule() }) {
            appendLine()
            appendLine("private fun ${Plural.FORM_FUNCTION}(count: kotlin.Number): kotlin.String =")
            appendLine("    io.github.octaviusframework.i18n.core.OctaviusI18n.selectPluralForm(\"${escapeString(lang)}\", count)")
        }
    }
}
