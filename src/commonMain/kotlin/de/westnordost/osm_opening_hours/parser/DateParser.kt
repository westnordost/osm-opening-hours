package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.AnnualEvent
import de.westnordost.osm_opening_hours.model.CalendarDate
import de.westnordost.osm_opening_hours.model.Date
import de.westnordost.osm_opening_hours.model.LastNth
import de.westnordost.osm_opening_hours.model.Month
import de.westnordost.osm_opening_hours.model.NextWeekday
import de.westnordost.osm_opening_hours.model.Nth
import de.westnordost.osm_opening_hours.model.PreviousWeekday
import de.westnordost.osm_opening_hours.model.SpecificWeekdayDate
import de.westnordost.osm_opening_hours.model.TWENTY_FOUR_SEVEN
import de.westnordost.osm_opening_hours.model.VariableDate
import de.westnordost.osm_opening_hours.model.WeekdayOffset

internal fun StringWithCursor.parseDate(lenient: Boolean, year: Int?): Date? {
    val initial = cursor

    val month = parseMonth(lenient)
    if (month != null) {
        skipWhitespaces(lenient)
        val specificWeekdayDate = parseSpecificWeekdayDate(lenient, year, month)
        if (specificWeekdayDate != null) return specificWeekdayDate

        val calendarDate = parseCalendarDate(lenient, year, month)
        if (calendarDate != null) return calendarDate

        cursor = initial
        return null
    }

    val event = parseAnnualEvent(lenient)
    if (event != null) {
        val weekdayOffset = parseWeekdayOffset(lenient)
        val dayOffset = parseDayOffset(lenient) ?: 0
        return VariableDate(year, event, weekdayOffset, dayOffset)
    } else {
        cursor = initial
        return null
    }
}

internal fun StringWithCursor.parseAnnualEvent(lenient: Boolean): AnnualEvent? {
    return AnnualEvent.entries.find { nextIsAndAdvance(it.osm, lenient) }
}

private fun StringWithCursor.parseSpecificWeekdayDate(
    lenient: Boolean, year: Int?, month: Month
): SpecificWeekdayDate? {
    val initial = cursor
    val weekday = parseWeekday(lenient) ?: return null
    skipWhitespaces(lenient)
    if (!nextIsAndAdvance('[')) {
        // e.g. "Jun Fr" - it's a Month + Weekday
        cursor = initial
        return null
    }
    skipWhitespaces(lenient)
    val minus = nextIsAndAdvance('-')
    skipWhitespaces(lenient)
    val nth = nextNumberAndAdvance(lenient, 1)?.toInt() ?: fail("Expected an nth")
    skipWhitespaces(lenient)
    if (!nextIsAndAdvance(']')) {
        // e.g. "Jun Fr[1,2]" or "Jun Fr[1-3]"
        // - it's not a SpecificWeekdayDate but Month + SpecificWeekdays
        cursor = initial
        return null
    }
    // we are done, but we need to look ahead to see if we actually parsed e.g.
    // "Jun Fr[1], Sa" - Month + SpecificWeekdays, any Weekday / Holiday
    if (nextIsWeekdayOrHolidaySelector(lenient)) {
        cursor = initial
        return null
    }
    val nthPointSelector = if (minus) LastNth(nth) else Nth(nth)

    val dayOffset = parseDayOffset(lenient) ?: 0

    return SpecificWeekdayDate(year, month, weekday, nthPointSelector, dayOffset)
}

private fun StringWithCursor.parseCalendarDate(
    lenient: Boolean, year: Int?, month: Month
): CalendarDate? {
    // Jan 05:00-08:00 should not be interpreted as Jan 05: 00:00-08:00
    // Jan 24/7 should also not be interpreted as Jan 24
    // so we need to look ahead first to see if there's a time
    if (nextIsHoursMinutes(lenient) || nextIs(TWENTY_FOUR_SEVEN)) return null
    val day = nextNumberAndAdvance(lenient, 2) ?: return null

    if (!lenient && day.length != 2) fail("Expected month day to consist of two digits")

    val weekdayOffset = parseWeekdayOffset(lenient)

    val dayOffset = parseDayOffset(lenient) ?: 0
    return CalendarDate(year, month, day.toInt(), weekdayOffset, dayOffset)
}

private fun StringWithCursor.parseWeekdayOffset(lenient: Boolean): WeekdayOffset? {
    val initial = cursor
    val isPositive = when {
        nextIsAndAdvance('+', lenient, skipWhitespaces = true) -> true
        nextIsAndAdvance('-', lenient, skipWhitespaces = true) -> false
        else -> return null
    }
    // no whitespace allowed, see https://wiki.openstreetmap.org/wiki/Key:opening_hours/specification#explain:date_offset:wday
    val weekday = parseWeekday(lenient)
    if (weekday == null) {
        cursor = initial
        return null
    }
    return if (isPositive) NextWeekday(weekday) else PreviousWeekday(weekday)
}

private fun StringWithCursor.nextIsWeekdayOrHolidaySelector(lenient: Boolean): Boolean {
    val initial = cursor
    if (nextIsCommaAndAdvance(lenient, skipWhitespaces = true)) {
        skipWhitespaces(lenient)
        if (parseWeekdaySelector(lenient) != null || parseHolidaySelector(lenient) != null) {
            cursor = initial
            return true
        }
    }
    cursor = initial
    return false
}
