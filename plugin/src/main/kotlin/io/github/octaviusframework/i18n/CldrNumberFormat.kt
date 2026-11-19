package io.github.octaviusframework.i18n

/** One locale's CLDR number format, as the runtime's `NumberFormat` takes it. */
internal data class CldrNumberFormat(
    val decimal: String,
    val group: String,
    val minus: String,
    val minimumGrouping: Int,
    val primaryGrouping: Int,
    val secondaryGrouping: Int,
    val digits: List<String>,
) {
    /** A Kotlin expression constructing the format. */
    fun toKotlin(): String {
        val core = "io.github.octaviusframework.i18n.core.NumberFormat"
        val digitList = if (digits == LATIN) "$core.LATIN_DIGITS" else "kotlin.collections.listOf(${digits.joinToString(", ") { literal(it) }})"
        return "$core(${literal(decimal)}, ${literal(group)}, ${literal(minus)}, $minimumGrouping, $primaryGrouping, $secondaryGrouping, $digitList)"
    }

    private companion object {
        val LATIN = (0..9).map { it.toString() }

        /** [text] as a Kotlin string literal, every character outside printable ASCII escaped, so it shows. */
        fun literal(text: String): String = text.map { ch ->
            if (ch.code in 0x20..0x7E) escapeString(ch.toString()) else "\\u%04X".format(ch.code)
        }.joinToString("", "\"", "\"")
    }
}
