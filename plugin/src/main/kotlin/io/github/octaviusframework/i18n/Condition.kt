package io.github.octaviusframework.i18n

/**
 * The condition of one form in a CLDR plural rule, such as `v = 0 and i % 10 = 2..4 and i % 100 != 12..14`:
 * relations joined by `and`, and those joined by `or`, which binds looser.
 */
internal sealed interface Condition {

    /** A Kotlin expression over the operands `o` that is true for the counts the condition takes. */
    fun toKotlin(): String

    /** Conditions joined by `or`. */
    class AnyOf(val parts: List<Condition>) : Condition {
        override fun toKotlin(): String = parts.joinToString(" || ") { it.toKotlin() }
    }

    /** Conditions joined by `and`. */
    class AllOf(val parts: List<Condition>) : Condition {
        override fun toKotlin(): String = parts.joinToString(" && ") { it.toKotlin() }
    }

    /**
     * An operand, optionally taken modulo [modulus], that is in one of [ranges], or not in any of them when
     * [negated]. `n` is the count itself: it is in a range only when it has no fraction other than zeros, and
     * its integer digits are.
     */
    class Relation(val operand: Char, val modulus: Long?, val negated: Boolean, val ranges: List<LongRange>) : Condition {

        override fun toKotlin(): String {
            val value = (if (operand == 'n') "o.i" else "o.$operand") + (modulus?.let { " % ${it}L" } ?: "")
            if (operand != 'n' && ranges.size == 1) {
                val range = ranges.single()
                val (equal, inRange) = if (negated) "!=" to "!in" else "==" to "in"
                return if (range.first == range.last) "$value $equal ${range.first}L" else "$value $inRange ${range.first}L..${range.last}L"
            }
            val any = ranges.joinToString(" || ") { range ->
                if (range.first == range.last) "$value == ${range.first}L" else "$value in ${range.first}L..${range.last}L"
            }
            val inAny = if (operand == 'n') "o.t == 0L && ${if (ranges.size == 1) any else "($any)"}" else any
            return if (negated) "!($inAny)" else "($inAny)"
        }
    }

    companion object {
        private val OPERANDS = setOf('n', 'i', 'v', 'w', 'f', 't', 'c', 'e')

        /** Parses a condition as CLDR writes it, with a space around every word and operator. */
        fun parse(text: String): Condition =
            AnyOf(text.trim().split(" or ").map { and -> AllOf(and.split(" and ").map { relation(it.trim()) }) })

        private fun relation(text: String): Relation {
            val tokens = text.split(Regex("""\s+"""))
            val (operand, modulus, rest) = when {
                tokens.size == 3 -> Triple(tokens[0], null, tokens.drop(1))
                tokens.size == 5 && tokens[1] == "%" -> Triple(tokens[0], tokens[2].toLong(), tokens.drop(3))
                else -> throw IllegalArgumentException("not a relation: $text")
            }
            require(operand.length == 1 && operand[0] in OPERANDS) { "not an operand: $operand in $text" }
            val negated = when (rest[0]) {
                "=" -> false
                "!=" -> true
                else -> throw IllegalArgumentException("not = or !=: ${rest[0]} in $text")
            }
            val ranges = rest[1].split(',').map { part ->
                val bounds = part.split("..")
                require(bounds.size in 1..2) { "not a range: $part in $text" }
                bounds.first().toLong()..bounds.last().toLong()
            }
            return Relation(operand[0], modulus, negated, ranges)
        }
    }
}
