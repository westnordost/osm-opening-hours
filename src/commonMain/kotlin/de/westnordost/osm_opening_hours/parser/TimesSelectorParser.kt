package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.*
import de.westnordost.osm_opening_hours.model.EventTime.*


private val eventTimeMap: Map<String, EventTime> = EventTime.entries.associateBy { it.osm }
private val eventTimeMaxLength: Int = eventTimeMap.keys.maxOf { it.length }

private val lenientEventTimeMap: Map<String, EventTime> = (
    // correct entries
    EventTime.entries.associateBy { it.osm } +
    // synonyms
    mapOf(
        "sundown" to Sunset,
        "sunup" to Sunrise
    )
).mapKeys { it.key.lowercase() }
private val lenientEventTimeMaxLength: Int = lenientEventTimeMap.keys.maxOf { it.length }

internal fun StringWithCursor.parseTimesSelector(lenient: Boolean): TimesSelector? {
    val startTime = parseTime(lenient) ?: return null

    val endTime = if (nextIsRangeAndAdvance(lenient)) {
        skipWhitespaces(lenient)
        parseExtendedTime(lenient) ?: fail("Expected an end time")
    } else null

    val step = if (endTime != null && nextIsAndAdvance('/', lenient, skipWhitespaces = true)) {
        skipWhitespaces(lenient)
        parseInterval() ?: fail("Expected an interval")
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

internal fun StringWithCursor.parseInterval(): Interval? {
    return parseClockTime(false) ?: parseIntervalMinutes()
}

internal fun StringWithCursor.parseIntervalMinutes(): IntervalMinutes? {
    val number = nextNumberAndAdvance() ?: return null
    return IntervalMinutes(number.toInt())
}

internal fun StringWithCursor.parseTime(lenient: Boolean): Time? {
    return parseVariableTime(lenient)
        ?: parseEventTime(lenient)?.let { VariableTime(it) }
        ?: parseClockTime(lenient)
}

internal fun StringWithCursor.parseExtendedTime(lenient: Boolean): ExtendedTime? {
    return parseVariableTime(lenient)
        ?: parseEventTime(lenient)?.let { VariableTime(it) }
        ?: parseExtendedClockTime(lenient)
}

internal fun StringWithCursor.parseVariableTime(lenient: Boolean): VariableTime? {
    if (!nextIsAndAdvance('(')) return null
    skipWhitespaces(lenient)
    val eventTime = parseEventTime(lenient) ?: fail("Expected an event time")
    skipWhitespaces(lenient)
    val op = when {
        nextIsAndAdvance('+') -> OffsetOp.Plus
        nextIsAndAdvance('-') -> OffsetOp.Minus
        else -> fail("Expected '+' or '-'")
    }
    skipWhitespaces(lenient)
    val offsetTime = parseClockTime(false) ?: fail("Expected an offset time")
    skipWhitespaces(lenient)
    if (!nextIsAndAdvance(')')) fail("Expected a ')'")
    return VariableTime(eventTime, TimeOffset(op, offsetTime))
}

internal fun StringWithCursor.parseClockTime(lenient: Boolean): ClockTime? {
    val (hour, minutes) = parseHourMinutes(lenient) ?: return null
    return ClockTime(hour, minutes ?: 0)
}

internal fun StringWithCursor.parseExtendedClockTime(lenient: Boolean): ExtendedClockTime? {
    val (hour, minutes) = parseHourMinutes(lenient) ?: return null
    return ExtendedClockTime(hour, minutes ?: 0)
}

internal fun StringWithCursor.parseHourMinutes(
    lenient: Boolean,
    allowWhitespacesAroundMinuteSeparator: Boolean = true
): Pair<Int, Int?>? {
    return if (lenient) parseHourMinutesLenient(allowWhitespacesAroundMinuteSeparator)
           else parseHourMinutesStrict(allowWhitespacesAroundMinuteSeparator)
}

private fun StringWithCursor.parseHourMinutesStrict(
    allowWhitespacesAroundMinuteSeparator: Boolean
): Pair<Int, Int?>? {
    val initial = cursor
    val hour = nextNumberAndAdvance(2) ?: return null
    if (hour.length != 2) {
        cursor = initial
        return null
    }
    if (allowWhitespacesAroundMinuteSeparator) skipWhitespaces(false)
    if (!nextIsAndAdvance(':')) {
        cursor = initial
        return null
    }
    if (allowWhitespacesAroundMinuteSeparator) skipWhitespaces(false)
    val minutes = nextNumberAndAdvance(2)
    if (minutes == null || minutes.length != 2) {
        cursor = initial
        return null
    }
    return Pair(hour.toInt(), minutes.toInt())
}

private fun StringWithCursor.parseHourMinutesLenient(
    allowWhitespacesAroundMinuteSeparator: Boolean
): Pair<Int, Int?>? {
    val initial = cursor
    if (nextIs(TWENTY_FOUR_SEVEN)) return null
    val hourStr = nextNumberAndAdvance(2) ?: return null

    if (allowWhitespacesAroundMinuteSeparator) skipWhitespaces(true)
    val minuteSeparator = nextIsAndAdvance {
        it == ':' || it == '.' || it.equals('h', ignoreCase = true)
    }
    var minutesStr: String? = null
    if (minuteSeparator != null) {
        if (allowWhitespacesAroundMinuteSeparator) skipWhitespaces(true)
        minutesStr = nextNumberAndAdvance(2)
        if (minutesStr == null && !minuteSeparator.equals('h', ignoreCase = true) ||
            minutesStr != null && minutesStr.length != 2
        ) {
            cursor = initial
            return null
        }
        skipWhitespaces(true)
    }

    var hour = hourStr.toInt()
    val clock12 = parseAmPm()
    if (clock12 != null && hour <= 12) {
        val isPm = clock12 == Clock12.PM
        when (hour) {
            12 -> hour = if (isPm) 12 else 0 // special handling for 12 AM / 12 PM
            else -> if (isPm) hour += 12
        }
    }
    retreatWhitespaces(true)

    return Pair(hour, minutesStr?.toInt())
}

internal enum class Clock12 { AM, PM }
internal fun StringWithCursor.parseAmPm(): Clock12? {
    if (nextIsAndAdvance('㏂')) return Clock12.AM
    if (nextIsAndAdvance('㏘')) return Clock12.PM

    val initial = cursor
    val clock12 = when {
        nextIsAndAdvance('a', ignoreCase = true) -> Clock12.AM
        nextIsAndAdvance('p', ignoreCase = true) -> Clock12.PM
        else -> return null
    }
    nextIsAndAdvance('.') // optional
    skipWhitespaces(true)
    if (!nextIsAndAdvance('m', ignoreCase = true)) {
        cursor = initial
        return null
    }

    skipWhitespaces(true)
    nextIsAndAdvance('.')
    retreatWhitespaces(true)
    return clock12
}

internal fun StringWithCursor.parseEventTime(lenient: Boolean): EventTime? {
    return if (lenient) parseEventTimeLenient() else parseEventTimeStrict()
}

internal fun StringWithCursor.parseEventTimeLenient(): EventTime? {
    val word = getNextKeyword(lenientEventTimeMaxLength)?.lowercase() ?: return null
    val event = lenientEventTimeMap[word] ?: return null
    advanceBy(word.length)
    return event
}

private fun StringWithCursor.parseEventTimeStrict(): EventTime? {
    val word = getNextKeyword(eventTimeMaxLength) ?: return null
    val event = eventTimeMap[word] ?: return null
    advanceBy(word.length)
    return event
}
