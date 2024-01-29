package de.westnordost.osm_opening_hours.model

import kotlin.jvm.JvmInline

sealed interface WeeksSelector

/** A single [week] */
@JvmInline
value class Week(val week: Int) : WeeksSelector {
    init {
        validateWeek("week", week)
    }

    operator fun rangeTo(end: Week) = WeekRange(this.week, end.week)
    override fun toString() = week.toString().padStart(2, '0')
}

/** Within a range of weeks ([start]-[end]) with an optional interval of [step] weeks.
 *  E.g. from week 12 to week 40 but only every second week */
data class WeekRange(val start: Int, val end: Int, val step: Int? = null) : WeeksSelector {
    init {
        validateWeek("start", start)
        validateWeek("end", end)
        if (step != null) {
            require(step > 0) { "step must be a positive integer but was $step" }
            require(step <= 53) { "step must be at most 53 but was $step" }
        }
    }

    override fun toString(): String {
        val start = start.toString().padStart(2, '0')
        val end = end.toString().padStart(2, '0')
        val step = if (step != null) "/$step" else ""
        return "$start-$end$step"
    }
}

private fun validateWeek(name: String, value: Int) {
    require(value > 0) { "$name must be greater than 0 but was $value" }
    require(value <= 53) { "$name must be at most 53 but was $value" }
}
