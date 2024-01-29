package de.westnordost.osm_opening_hours.model

sealed interface MonthsOrDateSelector

sealed interface Date : MonthsOrDateSelector {
    operator fun rangeTo(end: Date) = DateRange(this, end)
}

/** Single month. The ordinal 0 is January. */
enum class Month(internal val osm: String) {
    January("Jan"),
    February("Feb"),
    March("Mar"),
    April("Apr"),
    May("May"),
    June("Jun"),
    July("Jul"),
    August("Aug"),
    September("Sep"),
    October("Oct"),
    November("Nov"),
    December("Dec");

    operator fun rangeTo(end: Month) = MonthRange(this, end)
    override fun toString() = osm
}

/** Within range of months ([start]-[end])*/
data class MonthRange(val year: Int? = null, val start: Month, val end: Month) : MonthsOrDateSelector {
    constructor(start: Month, end: Month) : this(null, start, end)

    init {
        if (year != null) validateYear("year", year)
    }

    override fun toString() = sequenceOf(year, "$start-$end").joinNonEmptyStrings(" ")
}

/** A single [month], optionally in a specific [year] */
data class SingleMonth(val year: Int?, val month: Month) : MonthsOrDateSelector {
    constructor(month: Month) : this(null, month)

    init {
        if (year != null) validateYear("year", year)
    }

    override fun toString() = sequenceOf(year, month).joinNonEmptyStrings(" ")
}

/** A date consisting of an optional [year], a [day], [month] and an optional [weekdayOffset],
 *
 *  For example "August 3rd" or "Sunday after August 3rd 1999" */
data class CalendarDate(
    val year: Int?,
    val month: Month,
    val day: Int,
    val weekdayOffset: WeekdayOffset? = null
) : Date {
    constructor(month: Month, day: Int, weekdayOffset: WeekdayOffset? = null)
            : this(null, month, day, weekdayOffset)

    init {
        if (year != null) validateYear("year", year)
        validateMonthDay(day)
    }

    override fun toString() =
        sequenceOf(year, month, day.toString().padStart(2, '0'), weekdayOffset).joinNonEmptyStrings(" ")
}

/** A date consisting of optional [year], [month] and a specific weekday
 *
 *  For example "Second Friday in January 1999" or "Last Sunday in August" */
data class SpecificWeekdayDate(
    val year: Int?,
    val month: Month,
    val weekday: Weekday,
    val nthPointSelector: NthPointSelector
) : Date {
    constructor(month: Month, weekday: Weekday, nthPointSelector: NthPointSelector)
            : this(null, month, weekday, nthPointSelector)

    init {
        if (year != null) validateYear("year", year)
    }

    override fun toString() =
        if (year != null) "$year $month $weekday[$nthPointSelector]"
        else              "$month $weekday[$nthPointSelector]"
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

/** Since a [start] date with an optional offset */
data class StartingAtDate(val start: Date) : MonthsOrDateSelector {
    override fun toString() = "$start+"
}

/** Within a range of dates ([start]-[end])
 *
 *  For example "October 15 to March 01" */
data class DateRange(val start: Date, val end: Date) : MonthsOrDateSelector {
    override fun toString() = "$start-$end"
}

/** Several days within one month
 *
 * For example "January 3rd to 14th" or "July 3,5 and 7 in the year 1999" */
data class DatesInMonth(
    val year: Int?,
    val month: Month,
    val days: List<MonthDaySelector>,
) : MonthsOrDateSelector {
    constructor(year: Int?, month: Month, vararg days: MonthDaySelector)
            : this(year, month, days.toList())
    constructor(month: Month, vararg days: MonthDaySelector)
            : this(null, month, days.toList())

    init {
        if (year != null) validateYear("year", year)
    }

    override fun toString() =
        days.joinToString(",") { sequenceOf(year, month, it).joinNonEmptyStrings(" ") }
}

sealed interface MonthDaySelector
data class MonthDay(val day: Int) : MonthDaySelector {
    init {
        validateMonthDay(day)
    }

    operator fun rangeTo(end: Int) = MonthDayRange(day, end)

    override fun toString() = day.toString().padStart(2, '0')
}
data class MonthDayRange(val start: Int, val end: Int): MonthDaySelector {
    init {
        validateMonthDay(start)
        validateMonthDay(end)
        require(start < end) { "Start day must be smaller than end day" }
    }

    override fun toString() = sequenceOf(
        start.toString().padStart(2, '0'),
        "-",
        end.toString().padStart(2, '0')
    ).joinToString("")
}

sealed interface WeekdayOffset

/** Offset to next weekday */
data class NextWeekday(val weekday: Weekday) : WeekdayOffset {
    override fun toString() = "+$weekday"
}

/** Offset to next weekday */
data class PreviousWeekday(val weekday: Weekday) : WeekdayOffset {
    override fun toString() = "-$weekday"
}

private fun validateMonthDay(value: Int) {
    require(value > 0) { "Day must be greater than 0 but was $value" }
    require(value <= 31) { "Day must be at most 31 but was $value" }
}

internal fun MonthsOrDateSelector.hasYear(): Boolean = when (this) {
    is Date -> hasYear()
    is DateRange -> start.hasYear()
    is DatesInMonth -> year != null
    is SingleMonth -> year != null
    is MonthRange -> year != null
    is StartingAtDate -> start.hasYear()
}

private fun Date.hasYear(): Boolean = when (this) {
    is VariableDate -> year != null
    is CalendarDate -> year != null
    is SpecificWeekdayDate -> year != null
}

internal fun MonthsOrDateSelector.inYear(year: Int) : MonthsOrDateSelector {
    return when (this) {
        is Date -> inYear(year)
        is DateRange -> copy(start = start.inYear(year))
        is DatesInMonth -> copy(year = year)
        is SingleMonth -> copy(year = year)
        is MonthRange -> copy(year = year)
        is StartingAtDate -> copy(start = start.inYear(year))
    }
}

private fun Date.inYear(year: Int) : Date = when (this) {
    is CalendarDate -> copy(year = year)
    is VariableDate -> copy(year = year)
    is SpecificWeekdayDate -> copy(year = year)
}
