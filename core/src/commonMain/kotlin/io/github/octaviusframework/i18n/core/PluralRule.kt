package io.github.octaviusframework.i18n.core

interface PluralRule {
    fun selectForm(count: Int): String
    fun selectForm(count: Double): String
}
