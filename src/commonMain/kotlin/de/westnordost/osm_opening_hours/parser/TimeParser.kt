package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.ClockTime
import de.westnordost.osm_opening_hours.model.EventTime
import de.westnordost.osm_opening_hours.model.EventTime.Sunrise
import de.westnordost.osm_opening_hours.model.EventTime.Sunset
import de.westnordost.osm_opening_hours.model.ExtendedClockTime
import de.westnordost.osm_opening_hours.model.ExtendedTime
import de.westnordost.osm_opening_hours.model.OffsetOp
import de.westnordost.osm_opening_hours.model.TWENTY_FOUR_SEVEN
import de.westnordost.osm_opening_hours.model.Time
import de.westnordost.osm_opening_hours.model.TimeOffset
import de.westnordost.osm_opening_hours.model.VariableTime


private val eventTimeMap: Map<String, EventTime> by lazy {
    EventTime.entries.associateByTo(HashMap()) { it.osm }
}
private val eventTimeMaxLength: Int by lazy {
    eventTimeMap.keys.maxOf { it.length }
}

private val lenientEventTimeMap: Map<String, EventTime> by lazy {
    val map = HashMap<String, EventTime>()

    // correct entries
    EventTime.entries.associateByTo(map) { it.osm.lowercase() }
    // synonyms
    map["sundown"] = Sunset
    map["sunup"] = Sunrise

    map
}
private val lenientEventTimeMaxLength: Int by lazy {
    lenientEventTimeMap.keys.maxOf { it.length }
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
    val offsetTime = parseOffsetTime(lenient) ?: fail("Expected an offset time")
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

internal fun StringWithCursor.parseHourMinutes(
    lenient: Boolean,
    allowWhitespacesAroundMinuteSeparator: Boolean = true,
    allowAmPm: Boolean = true,
): Pair<Int, Int?>? =
    if (lenient) parseHourMinutesLenient(allowWhitespacesAroundMinuteSeparator, allowAmPm)
    else parseHourMinutesStrict(allowWhitespacesAroundMinuteSeparator)

private fun StringWithCursor.parseHourMinutesStrict(
    allowWhitespacesAroundMinuteSeparator: Boolean
): Pair<Int, Int?>? {
    val initial = cursor
    val hour = nextNumberAndAdvance(false, 2) ?: return null
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
    val minutes = nextNumberAndAdvance(false, 2)
    if (minutes == null || minutes.length != 2) {
        cursor = initial
        return null
    }
    return Pair(hour.toInt(), minutes.toInt())
}

private fun StringWithCursor.parseHourMinutesLenient(
    allowWhitespacesAroundMinuteSeparator: Boolean,
    allowAmPm: Boolean,
): Pair<Int, Int?>? {
    val initial = cursor
    if (nextIs(TWENTY_FOUR_SEVEN)) return null
    // allow up to three digits (but not four -> ambiguity with year numbers) to allow
    // unambiguous typos
    val hourStr = nextNumberAndAdvance(true, 3) ?: return null

    if (allowWhitespacesAroundMinuteSeparator) skipWhitespaces(true)
    val minuteSeparator = nextIsAndAdvance {
        it == ':' ||
            it == '.' ||
            it.equals('h', ignoreCase = true) ||
            it == '：' ||
            it == '時' // similar to 'h' but in Chinese/Japanese
    }
    val minutesStr: String?
    // lenient parsing requires a minute separator too (no "1200-1900") because otherwise
    // it could be ambiguous with year numbers
    if (minuteSeparator != null) {
        if (allowWhitespacesAroundMinuteSeparator) skipWhitespaces(true)
        minutesStr = nextNumberAndAdvance(true, 3)
        if (
        // don't allow a dangling ":"
            minutesStr == null && !minuteSeparator.equals('h', ignoreCase = true) && minuteSeparator != '時' ||
            // only allow anything other than 2 digits in minutes if the first digit is a 0 (e.g. "030", "0")
            // because e.g. "09:5" can be ambiguous (did he mean "09:50" or "09:05"?)
            minutesStr != null && minutesStr.length != 2 && minutesStr.first().digitToInt() != 0
        ) {
            cursor = initial
            return null
        }
        skipWhitespaces(true)
    } else {
        minutesStr = null
    }
    // ignore this character ("minutes") after minutes
    if (minutesStr != null) nextIsAndAdvance('分')

    var hour = hourStr.toInt()
    if (allowAmPm) {
        val clock12 = parseAmPm()
        if (clock12 != null && hour <= 12) {
            val isPm = clock12 == Clock12.PM
            when (hour) {
                12 -> hour = if (isPm) 12 else 0 // special handling for 12 AM / 12 PM
                else -> if (isPm) hour += 12
            }
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
