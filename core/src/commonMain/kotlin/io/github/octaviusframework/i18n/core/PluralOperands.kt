package io.github.octaviusframework.i18n.core

/**
 * The operands of a count that CLDR plural rules read, taken from the count as Kotlin prints it, so that the form
 * agrees with the number shown: `1.0` has one fraction digit, `1` none. The generated plural rules call it.
 *
 * A part longer than 18 digits keeps its last 18, raised above 10^18, which is all a rule tests.
 */
class PluralOperands private constructor(
    /** The integer digits. */
    val i: Long,
    /** How many fraction digits are shown. */
    val v: Long,
    /** How many fraction digits are shown, trailing zeros left out. */
    val w: Long,
    /** The fraction digits shown, as a number. */
    val f: Long,
    /** The fraction digits shown, as a number, trailing zeros left out. */
    val t: Long,
) {
    /** The exponent of compact notation, such as `1.2c6`, which a printed count does not use. */
    val e: Long get() = 0

    /** The same exponent under its other name. */
    val c: Long get() = 0

    companion object {
        private const val ABOVE = 1_000_000_000_000_000_000L

        /** The operands of [count] as it prints, or null when it prints with no digits, as `NaN` does. */
        fun of(count: Number): PluralOperands? = of(count.toString())

        /** The operands of a number written as [text], such as `1.50` or `1.0E10`, or null when it is not one. */
        internal fun of(text: String): PluralOperands? {
            val number = PrintedNumber.of(text) ?: return null
            val shown = number.fraction
            val trimmed = shown.trimEnd('0')
            return PluralOperands(value(number.integer), shown.length.toLong(), trimmed.length.toLong(), value(shown), value(trimmed))
        }

        private fun value(digits: String): Long {
            val significant = digits.trimStart('0')
            return when {
                significant.isEmpty() -> 0
                significant.length <= 18 -> significant.toLong()
                else -> ABOVE + significant.takeLast(18).toLong()
            }
        }
    }
}
