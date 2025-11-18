package de.westnordost.osm_opening_hours.model

sealed interface MonthsOrDateSelector

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
