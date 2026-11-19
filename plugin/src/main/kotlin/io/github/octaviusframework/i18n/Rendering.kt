package io.github.octaviusframework.i18n

/**
 * What a message is rendered with in one language: the parameter of each argument, what is inserted into the text
 * for each argument, and the forms the language's rule of each kind names.
 */
internal class Rendering(
    val parameters: Map<String, String>,
    val inserts: Map<String, String>,
    val ruleForms: (PluralKind) -> Set<String>,
)
