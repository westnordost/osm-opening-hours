package de.westnordost.osm_opening_hours.model

import kotlin.jvm.JvmInline


sealed interface TimesSelector

sealed interface TimePointsSelector : TimesSelector

sealed interface TimeSpansSelector : TimesSelector

sealed interface Interval

sealed interface Time : TimePointsSelector, ExtendedTime {
    operator fun rangeTo(end: ExtendedTime) = TimeSpan(this, end)
}

sealed interface ExtendedTime

/** A [hour]:[minutes] time as seen on a 24-hour clock */
data class ClockTime(val hour: Int, val minutes: Int = 0) : Time, Interval {
    init {
        require(hour in 0..23) { "hour must be within 0..23 but was $hour" }
        require(minutes in 0..59)  { "minutes must be within 0..59 but was $minutes" }
    }

    override fun toString() =
        "${hour.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}

/** A [hour]:[minutes] time as seen on a 48-hour clock, lol */
data class ExtendedClockTime(val hour: Int, val minutes: Int = 0) : ExtendedTime {
    init {
        require(hour in 0..47) { "hour must be within 0..47 but was $hour" }
        require(minutes in 0..59)  { "minutes must be within 0..59 but was $minutes" }
    }

    override fun toString() =
        "${hour.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}

/** The time of a daily event (e.g. sunrise) */
enum class EventTime(internal val osm: String) {
    Dawn("dawn"),
    Sunrise("sunrise"),
    Sunset("sunset"),
    Dusk("dusk");

    override fun toString() = osm

    operator fun plus(offset: ClockTime) = VariableTime(this, TimeOffset(OffsetOp.Plus, offset))
    operator fun minus(offset: ClockTime) = VariableTime(this, TimeOffset(OffsetOp.Minus, offset))
}

/** The time of a daily event (e.g. sunrise) plus/minus an offset (e.g. 1 hour after sunrise) */
data class VariableTime(
    val dailyEvent: EventTime,
    val timeOffset: TimeOffset? = null
) : Time {
    override fun toString() =
        if (timeOffset != null) "($dailyEvent$timeOffset)"
        else                    dailyEvent.toString()
}

data class TimeOffset(val op: OffsetOp, val offset: ClockTime) {
    override fun toString() = "$op$offset"
}

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
