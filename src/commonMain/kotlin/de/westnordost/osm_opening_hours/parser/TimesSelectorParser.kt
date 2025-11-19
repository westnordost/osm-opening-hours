package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.*
import de.westnordost.osm_opening_hours.model.EventTime.*


internal fun StringWithCursor.parseTimesSelector(lenient: Boolean): TimesSelector? {
    val startTime = parseTime(lenient) ?: return null

    val endTime = if (nextIsRangeAndAdvance(lenient)) {
        skipWhitespaces(lenient)
        parseExtendedTime(lenient) ?: fail("Expected an end time")
    } else null

    val step = if (endTime != null && nextIsAndAdvance('/', lenient, skipWhitespaces = true)) {
        skipWhitespaces(lenient)
        parseInterval(lenient) ?: fail("Expected an interval")
    } else null

    val openEnd = step == null && nextIsAndAdvance('+', lenient, skipWhitespaces = true)

    return when {
        step != null && endTime != null ->
            TimeIntervals(startTime, endTime, step)
        endTime != null ->
            TimeSpan(startTime, endTime, openEnd)
        openEnd ->
            StartingAtTime(startTime)
        else ->
            startTime
    }
}

internal fun StringWithCursor.parseInterval(lenient: Boolean): Interval? {
    return parseOffsetTime(lenient) ?: parseIntervalMinutes(lenient)
}

internal fun StringWithCursor.parseIntervalMinutes(lenient: Boolean): IntervalMinutes? {
    val number = nextNumberAndAdvance(lenient) ?: return null
    return IntervalMinutes(number.toInt())
}

internal fun StringWithCursor.parseOffsetTime(lenient: Boolean): ClockTime? {
    val initial = cursor
    val (hourStr, minutesStr) = parseHourMinutes(lenient) ?: return null
    // for offset time, minutes must be specified also in lenient mode because otherwise it would
    // be ambiguous with IntervalMinutes ("12:00-18:00/5" could otherwise be interpreted as
    // "12:00-18:00/5:00")
    if (minutesStr == null) {
        cursor = initial
        return null
    }
    return ClockTime(hourStr.toInt(), minutesStr.toInt())
}
