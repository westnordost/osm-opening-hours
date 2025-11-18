package de.westnordost.osm_opening_hours.model

sealed interface Date : MonthsOrDateSelector {
    operator fun rangeTo(end: Date) = DateRange(this, end)
}

/** A date consisting of an optional [year], a [day], [month] and an optional [weekdayOffset],
 *
 *  For example "August 3rd" or "Sunday after August 3rd 1999" */
data class CalendarDate(
    val year: Int?,
    val month: Month,
    val day: Int,
    val weekdayOffset: WeekdayOffset? = null,
    val dayOffset: Int = 0
) : Date {
    constructor(month: Month, day: Int, weekdayOffset: WeekdayOffset? = null, dayOffset: Int = 0)
        : this(null, month, day, weekdayOffset, dayOffset)

    init {
        if (year != null) validateYear("year", year)
        validateMonthDay(day)
    }

    override fun toString() =
        sequenceOf(
            year,
            month,
            day.toString().padStart(2, '0'),
            weekdayOffset,
            dayOffsetToString(dayOffset)
        ).joinNonEmptyStrings(" ")
}

/** A date consisting of optional [year], [month] and a specific weekday
 *
 *  For example "Second Friday in January 1999" or "Last Sunday in August" */
data class SpecificWeekdayDate(
    val year: Int?,
    val month: Month,
    val weekday: Weekday,
    val nthPointSelector: NthPointSelector,
    val dayOffset: Int = 0
) : Date {
    constructor(
        month: Month,
        weekday: Weekday,
        nthPointSelector: NthPointSelector,
        dayOffset: Int = 0
    ) : this(null, month, weekday, nthPointSelector, dayOffset)

    init {
        if (year != null) validateYear("year", year)
    }

    override fun toString() =
        sequenceOf(
            year,
            month,
            "$weekday[$nthPointSelector]",
            dayOffsetToString(dayOffset)
        ).joinNonEmptyStrings(" ")
}

/** An [annualEvent], optionally in a specific [year] and optional offsets
 *
 *  For example "Easter", "Tuesday after Easter 1999" or "2 days after Easter" */
data class VariableDate(
    val year: Int?,
    val annualEvent: AnnualEvent,
    val weekdayOffset: WeekdayOffset? = null,
    val dayOffset: Int = 0
): Date {
    constructor(
        annualEvent: AnnualEvent,
        weekdayOffset: WeekdayOffset? = null,
        dayOffset: Int = 0
    ) : this(null, annualEvent, weekdayOffset, dayOffset)

    init {
        if (year != null) validateYear("year", year)
    }

    override fun toString() = sequenceOf(
        year,
        annualEvent,
        weekdayOffset,
        dayOffsetToString(dayOffset)
    ).joinNonEmptyStrings(" ")
}

enum class AnnualEvent(internal val osm: String) {
    Easter("easter");

    override fun toString() = osm
}

private fun validateMonthDay(value: Int) {
    require(value > 0) { "Day must be greater than 0 but was $value" }
    require(value <= 31) { "Day must be at most 31 but was $value" }
}

internal fun Date.hasYear(): Boolean = when (this) {
    is VariableDate -> year != null
    is CalendarDate -> year != null
    is SpecificWeekdayDate -> year != null
}

internal fun Date.inYear(year: Int) : Date = when (this) {
    is CalendarDate -> copy(year = year)
    is VariableDate -> copy(year = year)
    is SpecificWeekdayDate -> copy(year = year)
}
