package de.westnordost.osm_opening_hours.model

import kotlin.jvm.JvmInline

sealed interface NthSelector

sealed interface NthPointSelector : NthSelector
sealed interface NthRangeSelector : NthSelector

/** The [nth] of something. E.g. the 4th Sunday in a month */
@JvmInline
value class Nth(val nth: Int) : NthPointSelector {
    init {
        validateNth("nth", nth)
    }

    operator fun rangeTo(end: Nth) = NthRange(this.nth, end.nth)
    override fun toString() = nth.toString()
}

/** Range of nths, e.g. only the first and second Sunday of a month */
data class NthRange(val start: Int, val end: Int) : NthRangeSelector {
    init {
        validateNth("start", start)
        validateNth("end", end)
        require(start < end) { "start $start must be smaller than end $end" }
    }

    override fun toString() = "$start-$end"
}

/** Only the last [nth] of something, e.g. the 2nd last Sunday of a month  */
data class LastNth(val nth: Int) : NthPointSelector {
    init {
        validateNth("nth", nth)
    }

    override fun toString() = "-$nth"
}

private fun validateNth(name: String, value: Int) {
    require(value in 1..5) { "$name must be within 1..5 but was $value" }
}
