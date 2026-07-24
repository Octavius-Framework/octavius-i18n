package io.github.octaviusframework.translations

/**
 * Represents the type of a translation entry in JSON.
 */
internal sealed class TranslationEntry {
    /** Simple text without parameters */
    data class Simple(val value: String) : TranslationEntry()

    /** Text with parameters {0}, {1}, ... */
    data class Parameterized(val value: String, val paramCount: Int) : TranslationEntry()

    /** Plural form (one/few/many) */
    data class Plural(val forms: Map<String, String>) : TranslationEntry()

    /** Nested object */
    data class Nested(val children: Map<String, TranslationEntry>) : TranslationEntry()
}

internal val PLURAL_KEYS = setOf("_zero", "_one", "_two", "_few", "_many", "_other")
internal val PARAM_REGEX = Regex("""\{(\d+)\}""")


/**
 * Converts snake_case or kebab-case to PascalCase.
 */
internal fun toPascalCase(input: String): String {
    return input.split("_", "-")
        .joinToString("") { word ->
            word.replaceFirstChar { it.uppercaseChar() }
        }
}

/**
 * Converts a string to camelCase (first letter lowercase).
 */
internal fun toCamelCase(input: String): String {
    val pascal = toPascalCase(input)
    return pascal.replaceFirstChar { it.lowercaseChar() }
}

/**
 * Escapes the name if it is a Kotlin keyword.
 */
internal fun escapeName(name: String): String {
    return if (isKotlinKeyword(name)) "`$name`" else name
}

/**
 * Checks if the name is a reserved Kotlin keyword.
 */
internal fun isKotlinKeyword(name: String): Boolean {
    val keywords = setOf(
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun",
        "if", "in", "interface", "is", "null", "object", "package", "return",
        "super", "this", "throw", "true", "try", "typealias", "typeof", "val",
        "var", "when", "while"
    )
    return name in keywords
}

internal fun mergeJsonMaps(target: MutableMap<String, Any?>, source: Map<String, Any?>) {
    for ((key, sourceValue) in source) {
        val targetValue = target[key]
        if (sourceValue is Map<*, *> && targetValue is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val newTarget = (targetValue as Map<String, Any?>).toMutableMap()
            @Suppress("UNCHECKED_CAST")
            mergeJsonMaps(newTarget, sourceValue as Map<String, Any?>)
            target[key] = newTarget
        } else {
            target[key] = sourceValue
        }
    }
}