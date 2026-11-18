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
        private val NUMBER = Regex("""-?(\d*)(?:\.(\d*))?(?:[eE]([+-]?\d+))?""")
        private const val ABOVE = 1_000_000_000_000_000_000L

        /** The operands of [count] as it prints, or null when it prints with no digits, as `NaN` does. */
        fun of(count: Number): PluralOperands? = of(count.toString())

        /** The operands of a number written as [text], such as `1.50` or `1.0E10`, or null when it is not one. */
        fun of(text: String): PluralOperands? {
            val match = NUMBER.matchEntire(text) ?: return null
            val whole = match.groupValues[1]
            val fraction = match.groupValues[2]
            if (whole.isEmpty() && fraction.isEmpty()) return null
            val exponent = match.groupValues[3].ifEmpty { "0" }.toInt()

            // Moving the point by the exponent, `1.5E3` is `1500` and `1.5E-3` is `0.0015`
            val digits = whole + fraction
            val point = whole.length + exponent
            val integer = when {
                point <= 0 -> ""
                point >= digits.length -> digits + "0".repeat(point - digits.length)
                else -> digits.substring(0, point)
            }
            val shown = when {
                point <= 0 -> "0".repeat(-point) + digits
                point >= digits.length -> ""
                else -> digits.substring(point)
            }
            val trimmed = shown.trimEnd('0')
            return PluralOperands(value(integer), shown.length.toLong(), trimmed.length.toLong(), value(shown), value(trimmed))
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
