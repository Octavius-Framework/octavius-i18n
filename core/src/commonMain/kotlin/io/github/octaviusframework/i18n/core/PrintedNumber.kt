package io.github.octaviusframework.i18n.core

/**
 * A number as it is written, such as `-1.50` or `1.0E10`: its sign, and its digits on either side of the point,
 * an exponent moved into place.
 */
internal class PrintedNumber(val negative: Boolean, val integer: String, val fraction: String) {

    companion object {
        private val NUMBER = Regex("""(-?)(\d*)(?:\.(\d*))?(?:[eE]([+-]?\d+))?""")

        /** [text] read as a number, or null when it is not one, as `NaN` is not. */
        fun of(text: String): PrintedNumber? {
            val match = NUMBER.matchEntire(text) ?: return null
            val whole = match.groupValues[2]
            val fraction = match.groupValues[3]
            if (whole.isEmpty() && fraction.isEmpty()) return null
            val exponent = match.groupValues[4].ifEmpty { "0" }.toInt()

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
            return PrintedNumber(match.groupValues[1].isNotEmpty(), integer, shown)
        }
    }
}
