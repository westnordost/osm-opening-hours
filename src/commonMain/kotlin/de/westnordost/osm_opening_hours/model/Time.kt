package de.westnordost.osm_opening_hours.model


sealed interface Time : TimePointsSelector, ExtendedTime {
    operator fun rangeTo(end: ExtendedTime) = TimeSpan(this, end)
}

/** A [hour]:[minutes] time as seen on a 24-hour clock. */
data class ClockTime(val hour: Int, val minutes: Int = 0) : Time, Interval {
    init {
        require(hour in 0..24) { "hour must be within 0..24 but was $hour" }
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

sealed interface ExtendedTime

/** A [hour]:[minutes] time as seen on a 48-hour clock. An extended time can be used to denote that a time range extends
 * into the next day, e.g. 18:00-28:00 (open from 18:00 until 4 hours after midnight). */
data class ExtendedClockTime(val hour: Int, val minutes: Int = 0) : ExtendedTime {
    init {
        require(hour in 0..48) { "hour must be within 0..48 but was $hour" }
        require(minutes in 0..59)  { "minutes must be within 0..59 but was $minutes" }
    }

    override fun toString() =
        "${hour.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}
