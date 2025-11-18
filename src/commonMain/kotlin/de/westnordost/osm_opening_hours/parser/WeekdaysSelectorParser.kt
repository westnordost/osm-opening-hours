package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.*
import kotlin.getValue

internal fun StringWithCursor.parseWeekdaySelector(lenient: Boolean): WeekdaysSelector? {
    val start = parseWeekday(lenient) ?: return null

    if (nextIsRangeAndAdvance(lenient)) {
        skipWhitespaces(lenient)
        val end = parseWeekday(lenient) ?: fail("Expected an end weekday")
        return WeekdayRange(start, end)
    }

    if (nextIsAndAdvance('[', lenient, skipWhitespaces = true)) {

        skipWhitespaces(lenient)
        val nths = parseCommaSeparated(lenient) { parseNthSelector(lenient) }.orEmpty()
        skipWhitespaces(lenient)

        if (!nextIsAndAdvance(']')) fail("Expected a ']'")

        val dayOffset = parseDayOffset(lenient)

        return SpecificWeekdays(start, nths, dayOffset ?: 0)
    }

    return start
}

internal fun StringWithCursor.parseDayOffset(lenient: Boolean): Int? {
    val initial = cursor
    skipWhitespaces(lenient)
    val op = when {
        nextIsAndAdvance('+') -> +1
        nextIsAndAdvance('-') -> -1
        else -> {
            cursor = initial
            return null
        }
    }
    skipWhitespaces(lenient)
    val days = nextNumberAndAdvance(lenient)
    if (days == null) {
        cursor = initial
        return null
    }
    skipWhitespaces(lenient)
    if (!nextIsAndAdvance("day", ignoreCase = lenient)) {
        cursor = initial
        return null
    }
    nextIsAndAdvance('s', ignoreCase = lenient)

    return op * days.toInt()
}
