package io.github.octaviusframework.i18n

/**
 * The source of `{objectName}Translations{Lang}.kt`: the translations of [keys] in [lang] compiled to Kotlin, one
 * function per key, implementing the translations interface, and each of the language's [rules] a key asks.
 */
internal fun languageSource(
    packageName: String,
    objectName: String,
    lang: String,
    keys: List<TranslationKey>,
    rules: Map<PluralKind, PluralRule>,
): String {
    val className = "${objectName}Translations${toPascalCase(lang)}"
    val ruleForms = { kind: PluralKind -> rules[kind]?.forms ?: setOf("other") }
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
        for (kind in PluralKind.entries) {
            if (keys.any { it.messages.getValue(lang).usesRule(kind, ruleForms) }) {
                appendLine()
                append(rules.getValue(kind).functionSource())
            }
        }
    }
}
