package io.github.octaviusframework.i18n

/**
 * The two kinds of CLDR plural rule: cardinal, for how many, written `$plural`, and ordinal, for which in order,
 * written `$ordinal`. Each language's rule of a kind is generated as the private function [function].
 */
internal enum class PluralKind(val use: Use, val function: String, val noun: String, file: String, type: String) {
    CARDINAL(Use.PLURAL, "___form", "plural form", "plurals.json", "plurals-type-cardinal"),
    ORDINAL(Use.ORDINAL, "___ordinal", "ordinal form", "ordinals.json", "plurals-type-ordinal");

    /** The discriminator that writes a choice of this kind. */
    val discriminator: String get() = use.discriminator!!

    /** CLDR's rules of this kind. */
    val cldr: CldrPlurals by lazy { CldrPlurals(this, file, type) }
}
