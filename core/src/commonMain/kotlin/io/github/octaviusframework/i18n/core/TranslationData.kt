package io.github.octaviusframework.i18n.core

/**
 * Interface for translation data of a specific language.
 */
interface TranslationData {
    val simple: Map<String, String>
    val plural: Map<String, PluralForms>
}
