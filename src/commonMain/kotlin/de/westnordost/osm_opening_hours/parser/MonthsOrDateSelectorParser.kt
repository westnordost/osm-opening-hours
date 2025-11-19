package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.*

internal fun StringWithCursor.parseMonthsOrDatesSelector(lenient: Boolean): MonthsOrDateSelector? {
    val initial = cursor
    val yearStr = nextNumberAndAdvance(lenient, 4)
    // not 4 digits -> not a year. Maybe something else, don't throw an exception and return cursor
    if (yearStr != null) {
        if (yearStr.length != 4) retreatBy(yearStr.length)
        else skipWhitespaces(lenient)
    }
    val year = yearStr?.toInt()

    val datesInMonth = parseDatesInMonth(lenient, year)
    if (datesInMonth != null) return datesInMonth

    val date = parseDate(lenient, year)
    if (date != null) {
        if (nextIsAndAdvance('+', lenient, skipWhitespaces = true)) {
            return StartingAtDate(date)
        }
        if (nextIsRangeAndAdvance(lenient)) {
            skipWhitespaces(lenient)
            val endYearStr = nextNumberAndAdvance(lenient, 4)
            // not 4 digits -> not a year. Maybe something else, don't throw an exception and return cursor
            if (endYearStr != null) {
                if (endYearStr.length != 4) retreatBy(endYearStr.length)
                else skipWhitespaces(lenient)
            }
            val endYear = endYearStr?.toInt()
            val endDate = parseDate(lenient, endYear) ?: fail("Expected end date")
            return DateRange(date, endDate)
        }
        return date
    }

    val month = parseMonth(lenient)
    if (month != null) {
        if (nextIsRangeAndAdvance(lenient)) {
            skipWhitespaces(lenient)
            val endMonth = parseMonth(lenient) ?: fail("Expected end month")
            return MonthRange(year, month, endMonth)
        } else {
            return SingleMonth(year, month)
        }
    } else {
        cursor = initial
        return null
    }
}

internal fun StringWithCursor.parseDatesInMonth(lenient: Boolean, year: Int?): DatesInMonth? {
    val initial = cursor
    val month = parseMonth(lenient) ?: return null
    skipWhitespaces(lenient)

    // Jan 05:00-08:00 should not be interpreted as Jan 05: 00:00-08:00
    // Jan 24/7 should also not be interpreted as Jan 24
    // so we need to look ahead first to see if there's a time
    if (nextIsHoursMinutes(lenient) || nextIs(TWENTY_FOUR_SEVEN)) {
        cursor = initial
        return null
    }

    val monthDays = parseCommaSeparated(lenient) { parseMonthDaySelector(lenient) }
    if (monthDays == null || monthDays.singleOrNull() is MonthDay) {
        // that's just a normal calendar date (and maybe something else following)!
        cursor = initial
        return null
    }
    // "Jun 01,02" is actually not in the spec (but "Jun 01-02" is)
    if (monthDays.size > 1 && !lenient) fail("List of month days not allowed in date(s) selector")
    return DatesInMonth(year, month, monthDays)
}

private fun StringWithCursor.parseMonthDaySelector(lenient: Boolean): MonthDaySelector? {
    val initial = cursor

    val day = nextNumberAndAdvance(lenient, 2) ?: return null
    if (!lenient && day.length != 2) fail("Expected month day to consist of two digits")

    if (nextIsRangeAndAdvance(lenient)) {
        skipWhitespaces(lenient)
        val endDay = nextNumberAndAdvance(lenient, 2)
        if (endDay == null) {
            cursor = initial
            return null
        }
        if (!lenient && endDay.length != 2) fail("Expected month day to consist of two digits")
        return MonthDayRange(day.toInt(), endDay.toInt())
    }
    return MonthDay(day.toInt())
}
