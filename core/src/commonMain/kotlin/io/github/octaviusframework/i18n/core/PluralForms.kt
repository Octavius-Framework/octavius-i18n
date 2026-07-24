package io.github.octaviusframework.i18n.core

/**
 * Represents plural forms for a given translation key.
 */
data class PluralForms(
    val zero: String?,
    val one: String?,
    val two: String?,
    val few: String?,
    val many: String?,
    val other: String
)
