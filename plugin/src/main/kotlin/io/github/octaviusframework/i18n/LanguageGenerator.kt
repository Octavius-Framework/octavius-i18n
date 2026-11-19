package io.github.octaviusframework.i18n

/** The private function in each language's file that writes a number in the language's number format. */
private const val NUMBER_FUNCTION = "___number"

/**
 * The source of `{objectName}Translations{Lang}.kt`: the translations of [keys] in [lang] compiled to Kotlin, one
 * function per key, implementing the translations interface, and each of the language's [rules] a key asks. A
 * number inserted into the text, a count or an argument marked `{name, number}`, is written in the language's CLDR
 * number format, which the object holds.
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
    var writesNumbers = false
    return buildString {
        // The IDE's inspections, on what the generator writes on purpose: full names no type of the target package
        // can shadow, `___` functions no key can take, every inserted argument in braces, and CLDR's conditions as
        // CLDR writes them, some of which repeat a test, as English ordinals do. Overrides, named after keys, are
        // not checked
        appendLine(
            "@file:Suppress(\"RemoveRedundantQualifierName\", \"FunctionName\", \"RemoveCurlyBracesFromTemplate\", " +
                "\"KotlinConstantConditions\")"
        )
        appendLine()
        appendLine("package $packageName")
        appendLine()
        appendLine("/**")
        appendLine(" * The translations of [$objectName] in `$lang`.")
        appendLine(" *")
        appendLine(" * This file is auto-generated. Do not edit manually.")
        appendLine(" */")
        appendLine("internal object $className : ${objectName}Translations {")
        appendLine()
        appendLine("    /** The number format of `$lang`, CLDR ${CldrNumbers.version}'s. */")
        appendLine("    override val numberFormat: io.github.octaviusframework.i18n.core.NumberFormat =")
        appendLine("        ${CldrNumbers.formatFor(lang).toKotlin()}")
        appendLine()
        appendLine("    /** The accessors of [$objectName] in `$lang`, made the first time [$objectName.inLanguage] gives them. */")
        appendLine("    override val ___accessors: ${objectName}Accessors")
        appendLine("        by kotlin.lazy(kotlin.LazyThreadSafetyMode.PUBLICATION) { ${objectName}Accessors { this } }")
        for (key in keys) {
            val message = key.messages.getValue(lang)
            val counts = key.signature.parameters.filter { it.type == "kotlin.Number" }.map { it.argument }.toSet()
            val inserted = mutableMapOf<String, MutableSet<Use>>().also { message.collectUses(it) }
                .filterValues { Use.TEXT in it || Use.NUMBER in it }.keys
            if (counts.any { it in inserted }) writesNumbers = true
            val inserts = key.signature.parameters.associate {
                it.argument to if (it.argument in counts) "$NUMBER_FUNCTION(${it.name})" else it.name
            }
            appendLine()
            appendLine(
                "    override fun ${memberName(key.path)}(${key.signature.declaration()}): kotlin.String = " +
                    message.toKotlin(Rendering(key.signature.names(), inserts, ruleForms), "    ")
            )
        }
        appendLine("}")
        for (kind in PluralKind.entries) {
            if (keys.any { it.messages.getValue(lang).usesRule(kind, ruleForms) }) {
                appendLine()
                append(rules.getValue(kind).functionSource())
            }
        }
        if (writesNumbers) {
            appendLine()
            appendLine("/** [count] in the number format of `$lang`. */")
            appendLine("private fun $NUMBER_FUNCTION(count: kotlin.Number): kotlin.String = $className.numberFormat.format(count)")
        }
    }
}
