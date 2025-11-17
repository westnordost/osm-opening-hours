package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.*
import de.westnordost.osm_opening_hours.model.Month.*

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

private fun StringWithCursor.parseCalendarDate(
    lenient: Boolean, year: Int?, month: Month
): CalendarDate? {
    // Jan 05:00-8:00 should not be interpreted as Jan 05: 00:00-08:00
    // Jan 24/7 should also not be interpreted as Jan 24
    // so we need to look ahead first to see if there's a time
    if (nextIsClockTime(lenient) || nextIs(TWENTY_FOUR_SEVEN)) return null
    val day = nextNumberAndAdvance(lenient, 2) ?: return null

    if (!lenient && day.length != 2) fail("Expected month day to consist of two digits")

    val weekdayOffset = parseWeekdayOffset(lenient)

    val dayOffset = parseDayOffset(lenient) ?: 0
    return CalendarDate(year, month, day.toInt(), weekdayOffset, dayOffset)
}

private fun StringWithCursor.nextIsClockTime(lenient: Boolean): Boolean {
    val initial = cursor
    val (_, minutes) = parseHourMinutes(lenient, allowWhitespacesAroundMinuteSeparator = false)
        ?: return false
    cursor = initial
    // only if hours + minutes are defined
    return minutes != null
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

internal fun StringWithCursor.parseDatesInMonth(lenient: Boolean, year: Int?): DatesInMonth? {
    val initial = cursor
    val month = parseMonth(lenient) ?: return null
    skipWhitespaces(lenient)
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


internal fun StringWithCursor.parseAnnualEvent(lenient: Boolean): AnnualEvent? {
    return AnnualEvent.entries.find { nextIsAndAdvance(it.osm, lenient) }
}

internal fun StringWithCursor.parseMonth(lenient: Boolean): Month? {
    return if (lenient) parseMonthLenient() else parseMonthStrict()
}

private val monthsMap: Map<String, Month> = Month.entries.associateBy { it.osm }
private val monthsMaxLength: Int = monthsMap.keys.maxOf { it.length }

private fun StringWithCursor.parseMonthStrict(): Month? {
    val word = getNextKeyword(monthsMaxLength) ?: return null
    val month = monthsMap[word] ?: return null
    advanceBy(word.length)
    return month
}

private val lenientMonthsMap: Map<String, Month> by lazy {
    val map = HashMap<String, Month>()

    // correct 3-letter abbreviations
    Month.entries.associateByTo(map) { it.osm.lowercase() }

    // full English names
    Month.entries.associateByTo(map) { it.name }

    val namesLists = listOf(
        // germanic
        listOf("jan", "feb", "mär", "apr", "mai", "jun", "jul", "aug", "sep", "okt", "nov", "dez"), // de
        listOf("jan", "feb", "mrt", "apr", "mei", "jun", "jul", "aug", "sep", "okt", "nov", "dec"), // nl
        listOf("jan", "feb", "mrt", "apr", "mei", "jun", "jul", "aug", "sep", "okt", "nov", "des"), // af
        listOf("jan", "feb", "mar", "apr", "maj", "jun", "jul", "aug", "sep", "okt", "nov", "dec"), // da
        listOf("jan", "feb", "mar", "apr", "mai", "jun", "jul", "aug", "sep", "okt", "nov", "des"), // nb
        listOf("jan", "feb", "mars", "apr", "mai", "juni", "juli", "aug", "sep", "okt", "nov", "des"), // nn
        listOf("jan", "feb", "mars", "apr", "maj", "juni", "juli", "aug", "sep", "okt", "nov", "dec"), // sv

        // romance
        listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sept", "oct", "nov", "dic"), // es
        listOf("gen", "feb", "mar", "apr", "mag", "giu", "lug", "ago", "set", "ott", "nov", "dic"), // it
        listOf("ian", "feb", "mar", "apr", "mai", "iun", "iul", "aug", "sept", "oct", "nov", "dec"), // ro
        listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"), // pt
        listOf("janv", "févr", "mars", "avr", "mai", "juin", "juil", "août", "sept", "oct", "nov", "déc"), // fr

        // slavic
        listOf("янв", "февр", "март", "апр", "май", "июнь", "июль", "авг", "сент", "окт", "нояб", "дек"), // ru
        listOf("січ", "лют", "бер", "кві", "тра", "чер", "лип", "сер", "вер", "жов", "лис", "гру"), // uk
    )
    for (names in namesLists) {
        for (i in 0 ..< 12) map[names[i]] = Month.entries[i]
    }

    map
}

private val lenientMonthsMaxLength: Int by lazy {
    lenientMonthsMap.keys.maxOf { it.length }
}

private fun StringWithCursor.parseMonthLenient(): Month? {
    val word = getNextKeyword(lenientMonthsMaxLength)?.lowercase() ?: return null
    val event = lenientMonthsMap[word] ?: return null
    advanceBy(word.length)
    nextIsAndAdvance('.')
    return event
}
