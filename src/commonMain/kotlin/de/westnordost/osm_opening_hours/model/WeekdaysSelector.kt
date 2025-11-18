package de.westnordost.osm_opening_hours.model

sealed interface WeekdaysSelector

/** A single weekday. The ordinal 0 is Monday. */
enum class Weekday(internal val osm: String) : WeekdaysSelector {
    Monday("Mo"),
    Tuesday("Tu"),
    Wednesday("We"),
    Thursday("Th"),
    Friday("Fr"),
    Saturday("Sa"),
    Sunday("Su");

    override fun toString() = osm
    operator fun rangeTo(end: Weekday) = WeekdayRange(this, end)
}

/** Within a range of weekdays ([start]-[end]), may loop over the end of the week */
data class WeekdayRange(val start: Weekday, val end: Weekday) : WeekdaysSelector {
    override fun toString() = "$start-$end"
}

/** At a specific [weekday] (s) within a month: Only the [nths] weekday. E.g. only the second
 *  Sunday. Optionally, moreover specify an offset to that. E.g. only the day after the second
 *  Sunday. */
data class SpecificWeekdays(
    val weekday: Weekday,
    val nths: List<NthSelector>,
    val dayOffset: Int = 0
) : WeekdaysSelector {

    init {
        require(nths.isNotEmpty()) { "nths must not be empty" }
    }

    override fun toString() = sequenceOf(
        "$weekday[${nths.joinToString(",")}]",
        dayOffsetToString(dayOffset)
    ).joinNonEmptyStrings(" ")
}
