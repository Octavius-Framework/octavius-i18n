package io.github.octaviusframework.i18n

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

/**
 * A language code in the one spelling the plugin compares codes by: lower case, with `-` between its parts, so
 * `pt_BR` and `PT-br` are both `pt-br`.
 */
internal fun languageCode(code: String): String = code.lowercase().replace('_', '-')

/**
 * The one of [languages] that [code] finds, as the generated lookup finds a file: the code itself, then the code
 * with its last part cut off, as many times as it has parts, so `en-US` finds `en`.
 */
internal fun findLanguage(code: String, languages: List<String>): String? {
    val byCode = languages.associateBy { languageCode(it) }
    var shorter = languageCode(code)
    while (true) {
        byCode[shorter]?.let { return it }
        val cut = shorter.lastIndexOf('-')
        if (cut < 0) return null
        shorter = shorter.substring(0, cut)
    }
}

/**
 * Escapes a string for use inside a Kotlin string literal.
 */
internal fun escapeString(s: String): String {
    return s.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
        .replace("$", "\\$")
}
