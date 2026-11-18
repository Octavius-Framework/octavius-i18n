package io.github.octaviusframework.i18n.core

/**
 * How a language writes a number: its decimal separator, its grouping separator and where groups fall, its minus
 * sign and its ten [digits]. The generated code gives each language its format from CLDR.
 *
 * @property minimumGrouping How many digits the integer part needs beyond the first group to be grouped at all: 2
 *   keeps `1000` whole and groups `10 000`, as Polish does.
 * @property primaryGrouping The size of the group nearest the decimal separator, or 0 for no grouping.
 * @property secondaryGrouping The size of every group before it: 2 in Hindi's `12,34,567`.
 */
class NumberFormat(
    val decimal: String = ".",
    val group: String = ",",
    val minus: String = "-",
    val minimumGrouping: Int = 1,
    val primaryGrouping: Int = 3,
    val secondaryGrouping: Int = primaryGrouping,
    val digits: List<String> = LATIN_DIGITS,
) {
    init {
        require(digits.size == 10) { "a number format has ten digits, not ${digits.size}" }
    }

    /**
     * [count] written in this format, with the digits Kotlin prints for it: `1.5` is `1,5` and `10000` is `10 000`
     * in Polish. A count that prints with no digits, as `NaN` does, is returned as it prints.
     */
    fun format(count: Number): String {
        val number = PrintedNumber.of(count.toString()) ?: return count.toString()
        val integer = number.integer.trimStart('0').ifEmpty { "0" }
        return buildString {
            if (number.negative) append(minus)
            append(grouped(integer))
            if (number.fraction.isNotEmpty()) {
                append(decimal)
                number.fraction.forEach { append(digits[it - '0']) }
            }
        }
    }

    private fun grouped(integer: String): String {
        val mapped = integer.map { digits[it - '0'] }
        if (primaryGrouping <= 0 || integer.length < primaryGrouping + minimumGrouping) return mapped.joinToString("")
        val groups = ArrayDeque<List<String>>()
        var end = mapped.size
        var size = primaryGrouping
        while (end > 0) {
            val start = maxOf(0, end - size)
            groups.addFirst(mapped.subList(start, end))
            end = start
            size = if (secondaryGrouping > 0) secondaryGrouping else primaryGrouping
        }
        return groups.joinToString(group) { it.joinToString("") }
    }

    companion object {
        /** The digits `0` to `9`. */
        val LATIN_DIGITS: List<String> = (0..9).map { it.toString() }
    }
}
