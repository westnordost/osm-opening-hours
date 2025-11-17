package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.*
import de.westnordost.osm_opening_hours.model.TWENTY_FOUR_SEVEN

internal fun StringWithCursor.parseSelector(lenient: Boolean): Selector {
    if (nextIsAndAdvance(TWENTY_FOUR_SEVEN)) {
        if (!lenient && nextIsAndAdvance(',', lenient, skipWhitespaces = true)) {
            fail("Did not expect the beginning of a new additional rule here")
        }
        return TwentyFourSeven
    }

    // 20% performance improvement: try the most common case first - Weekdays + times
    val (shortcutWeekdaysAndHolidays, shortcutTimes) = parseWeekdaysAndTimes(lenient)
    if (shortcutWeekdaysAndHolidays != null || shortcutTimes != null) {
        if (!lenient && shortcutTimes == null && nextIsAndAdvance(',', lenient, skipWhitespaces = true)) {
            fail("Did not expect the beginning of a new additional rule here")
        }
        return Range(
            weekdays = shortcutWeekdaysAndHolidays?.weekdays,
            holidays = shortcutWeekdaysAndHolidays?.holidays,
            isRestrictedByHolidays = shortcutWeekdaysAndHolidays?.isRestrictedByHolidays ?: false,
            times = shortcutTimes
        )
    }

    var years: List<YearsSelector>? = null
    var months: MutableList<MonthsOrDateSelector>? = null
    var weeks: List<WeeksSelector>? = null
    var separatorForReadability: Boolean? = null

    val initial = cursor
    val comment = parseComment()
    if (comment != null) {
        skipWhitespaces(lenient)
        // a ':' is mandatory when using a comment instead of a wide selector because without
        // it, the comment will actually be the comment within a <rule_modifier>. E.g.
        // "During ramadan": 20:00-22:00      vs     "By appointment only"
        if (!nextIsAndAdvance(":")) {
            // since we already advanced, we need to go back
            cursor = initial
            return Range()
        }
    } else {
        years = parseCommaSeparated(lenient) { parseYearsSelector(lenient) }
        skipWhitespaces(lenient)
        months = parseCommaSeparated(lenient) { parseMonthsOrDatesSelector(lenient) }?.toMutableList()

        // a year that actually should rather belong to the months selector:
        // only if it is a single year (not a list etc.), the first months selector does not already
        // have a year and - if there are other months selectors - any have a year specified
        val singleYear = years?.singleOrNull() as? Year
        val firstMonth = months?.firstOrNull()
        val otherMonths =
            if (months == null || months.size < 2) emptyList()
            else months.subList(1, months.size)
        if (singleYear != null &&
            firstMonth?.hasYear() == false &&
            (otherMonths.isEmpty() || otherMonths.any { it.hasYear() })
            ) {
            months?.set(0, firstMonth.inYear(singleYear.year))
            years = null
        }

        skipWhitespaces(lenient)
        if (nextIsAndAdvance(WEEK, ignoreCase = lenient)) {
            skipWhitespaces(lenient)
            weeks = parseCommaSeparated(lenient) { parseWeeksSelector(lenient) }
            skipWhitespaces(lenient)
        }
        skipWhitespaces(lenient)
        // this is not mentioned in the specification, but both the reference implementation and
        // ch.simonpoole.OpeningHoursParser allow a dangling "," at the end of the wide range
        // selectors. The former does not even correct it for the canonical version! Nevertheless,
        // it is an error, so let's treat it as such in non-lenient mode
        if (nextIsAndAdvance(',', lenient, skipWhitespaces = true)) {
            if (!lenient) fail("Did not expect the beginning of a new additional rule here")
        }

        separatorForReadability =
            (!lenient && nextIsAndAdvance(':'))
            || (nextIsAndAdvance { it == ':' || it == '：' } != null)
    }
    skipWhitespaces(lenient)
    val (weekdaysAndHolidays, times) = parseWeekdaysAndTimes(lenient)

    val isWideAndShortRange =
        (years != null || months != null || weeks != null) && (weekdaysAndHolidays != null || times != null)
    if (separatorForReadability != true && !isWideAndShortRange) {
        separatorForReadability = null
    }

    if (!lenient && times == null && nextIsAndAdvance(',', lenient, skipWhitespaces = true)) {
        fail("Did not expect the beginning of a new additional rule here")
    }

    retreatWhitespaces(lenient)

    return if (comment != null) {
        Range(
            text = comment,
            weekdays = weekdaysAndHolidays?.weekdays,
            holidays = weekdaysAndHolidays?.holidays,
            isRestrictedByHolidays = weekdaysAndHolidays?.isRestrictedByHolidays ?: false,
            times = times
        )
    } else {
        Range(
            years,
            months,
            weeks,
            text = null,
            separatorForReadability,
            weekdaysAndHolidays?.weekdays,
            weekdaysAndHolidays?.holidays,
            weekdaysAndHolidays?.isRestrictedByHolidays ?: false,
            times
        )
    }
}

private fun StringWithCursor.parseWeekdaysAndTimes(
    lenient: Boolean
): Pair<WeekdaysAndHolidays?, List<TimesSelector>?> {
    val weekdays = parseWeekdaysAndHolidaysSelector(lenient)
    var ws = 0
    if (weekdays != null) {
        ws = skipWhitespaces(lenient)
        if (lenient && nextIsAndAdvance { it == ':' || it == '：' } != null) {
            ws = skipWhitespaces(lenient)
        }
    }
    var times = parseCommaSeparated(lenient) { parseTimesSelector(lenient) }
    if (times == null) {
        if (lenient && nextIsAndAdvance(TWENTY_FOUR_SEVEN)) {
            times = listOf(ClockTime(0)..ExtendedClockTime(24))
        } else {
            retreatBy(ws)
        }
    }
    return Pair(weekdays, times)
}

internal fun StringWithCursor.parseComment(): String? {
    if (!nextIsAndAdvance('"')) return null
    val endPos = findNext('"')
    val result = advanceBy(endPos)
    if (!nextIsAndAdvance('"')) fail("Missing '\"'")
    return result
}

internal data class WeekdaysAndHolidays(
    val weekdays: List<WeekdaysSelector>? = null,
    val holidays: List<HolidaySelector>? = null,
    val isRestrictedByHolidays: Boolean = false,
)

internal fun StringWithCursor.parseWeekdaysAndHolidaysSelector(
    lenient: Boolean
): WeekdaysAndHolidays? {
    val days = parseCommaSeparated(lenient) { parseWeekdayAndHolidaySelector(lenient) } ?: return null

    val onlyHolidays = days.filterIsInstance<HolidaySelector>()
    val isOnlyHolidays = onlyHolidays.size == days.size
    if (isOnlyHolidays) {
        val ws = skipWhitespaces(lenient)

        val weekdays = if (ws > 0) {
            parseCommaSeparated(lenient) { parseWeekdaySelector(lenient) }
        } else null

        if (weekdays != null) {
            return WeekdaysAndHolidays(weekdays, onlyHolidays, true)
        } else {
            retreatBy(ws)
            return WeekdaysAndHolidays(null, onlyHolidays, false)
        }
    }

    if (!lenient && !allHolidaysAreAtOneEnd(days)) {
        fail("Holidays must either be at the start or end")
    }
    return WeekdaysAndHolidays(
        days.filterIsInstance<WeekdaysSelector>().takeIf { it.isNotEmpty() },
        days.filterIsInstance<HolidaySelector>().takeIf { it.isNotEmpty() },
        false
    )
}

private fun allHolidaysAreAtOneEnd(days: List<Any>): Boolean {
    return allHolidaysAreInFront(days) || allHolidaysAreInFront(days.asReversed())
}

private fun allHolidaysAreInFront(days: Iterable<Any>): Boolean {
    var noHolidaysFromHereOn = false
    for (day in days) {
        when (day) {
            is WeekdaysSelector -> noHolidaysFromHereOn = true
            is HolidaySelector -> if (noHolidaysFromHereOn) return false
            else -> throw IllegalStateException()
        }
    }
    return true
}

private fun StringWithCursor.parseWeekdayAndHolidaySelector(lenient: Boolean): Any? =
    parseWeekdaySelector(lenient) ?: parseHolidaySelector(lenient)
