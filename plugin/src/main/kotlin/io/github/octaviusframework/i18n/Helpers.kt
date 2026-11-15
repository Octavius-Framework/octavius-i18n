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
