package de.westnordost.osm_opening_hours.model

import kotlin.jvm.JvmInline


sealed interface TimesSelector

sealed interface TimePointsSelector : TimesSelector

sealed interface TimeSpansSelector : TimesSelector

sealed interface Interval

/** At intervals of the given [step] in-between the time range from [start] to [end]. */
data class TimeIntervals(
    val start: Time,
    val end: ExtendedTime,
    val step: Interval
) : TimePointsSelector {
    override fun toString() = "$start-$end/$step"
}

/** Time interval specified in [minutes] */
@JvmInline value class IntervalMinutes(val minutes: Int) : Interval {
    init {
        require(minutes > 0) { "Interval in minutes must be a positive integer but was $minutes" }
    }

    override fun toString() = minutes.toString()
}

/** From [start] to [end] time, optionally with open end */
data class TimeSpan(
    val start: Time,
    val end: ExtendedTime,
    val openEnd: Boolean = false
) : TimeSpansSelector {

    override fun toString(): String {
        var beautifiedEnd = end
        if (end is ExtendedClockTime) {
            val endMin = end.hour * 60 + end.minutes
            if (start is ClockTime) {
                val startMin = start.hour * 60 + start.minutes
                val hour24 = 24 * 60
                // 16:00-30:00  =>  16:00-06:00  (but not 16:00-40:00  =>  16:00-16:00)
                if (endMin > hour24 && endMin - startMin < hour24) {
                    beautifiedEnd = end.copy(hour = end.hour - 24)
                }
                // 08:00-00:00  =>  08:00-24:00  (but not 00:00-00:00  =>  00:00-24:00)
                else if (endMin == 0 && startMin > 0) {
                    beautifiedEnd = ExtendedClockTime(24)
                }
            } else if (endMin == 0) {
                beautifiedEnd = ExtendedClockTime(24)
            }
        }

        return "$start-$beautifiedEnd${if (openEnd) "+" else ""}"
    }
}

/** From [start] time with open end */
data class StartingAtTime(val start: Time) : TimeSpansSelector {
    override fun toString() = "$start+"
}

enum class OffsetOp(val s: String) {
    Plus("+"), Minus("-");

    override fun toString() = s
}
