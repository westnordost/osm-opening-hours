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

internal fun StringWithCursor.parseEventTime(lenient: Boolean): EventTime? {
    return if (lenient) parseEventTimeLenient() else parseEventTimeStrict()
}

internal fun StringWithCursor.parseEventTimeLenient(): EventTime? {
    val word = getNextWord(lenientEventTimeMaxLength) { it.isLetter() }?.lowercase() ?: return null
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

internal fun StringWithCursor.parseClockTime(lenient: Boolean): ClockTime? {
    val (hour, minutes) = parseClock(lenient) ?: return null
    return ClockTime(hour, minutes ?: 0)
}

internal fun StringWithCursor.parseExtendedClockTime(lenient: Boolean): ExtendedClockTime? {
    val (hour, minutes) = parseClock(lenient) ?: return null
    return ExtendedClockTime(hour, minutes ?: 0)
}

private fun StringWithCursor.parseClock(lenient: Boolean): Pair<Int, Int?>? {
    val (hourStr, minutesStr) = parseHourMinutes(lenient) ?: return null
    val hour = hourStr.toInt()
    val minutes = minutesStr?.toInt()

    if (lenient) {
        skipWhitespaces(lenient)
        val clock12 = parseAmPm()
        retreatWhitespaces(lenient)
        if (clock12 != null && hour <= 12) {
            val isPm = clock12 == Clock12.PM
            val newHour = when (hour) {
                12 -> if (isPm) 12 else 0 // special handling for 12 AM / 12 PM
                else -> if (isPm) hour + 12 else hour
            }
            return Pair(newHour, minutes)
        }
    }
    return Pair(hour, minutes)
}

internal fun StringWithCursor.parseHourMinutes(lenient: Boolean): Pair<String, String?>? =
    if (lenient) parseHourMinutesLenient() else parseHourMinutesStrict()

private fun StringWithCursor.parseHourMinutesStrict(): Pair<String, String>? {
    val initial = cursor
    val hour = nextNumberAndAdvance(false, 2) ?: return null
    if (hour.length != 2) {
        cursor = initial
        return null
    }
    skipWhitespaces(false)
    if (!nextIsAndAdvance(':')) {
        cursor = initial
        return null
    }
    skipWhitespaces(false)
    val minutes = nextNumberAndAdvance(false, 2)
    if (minutes == null || minutes.length != 2) {
        cursor = initial
        return null
    }
    return Pair(hour, minutes)
}

private fun StringWithCursor.parseHourMinutesLenient(): Pair<String, String?>? {
    if (nextIs(TWENTY_FOUR_SEVEN)) return null

    val initial = cursor
    // allow up to three digits (but not four -> ambiguity with year numbers) to allow unambiguous typos
    val hourStr = nextNumberAndAdvance(true, 3) ?: return null

    skipWhitespaces(true)

    // a character that denotes the end of the hours (e.g. 12h), without implying that minutes will follow
    val hasHoursEndChar =
        nextIsAndAdvance { it == 'h' || it == 'H' || it == '時' } != null

    val hasHoursMinutesSeparator =
        !hasHoursEndChar && nextIsAndAdvance { it == ':' || it == '.' || it == '：' } != null

    val minutesStr: String?
    // lenient parsing requires a minute separator, too, because otherwise
    // it could be ambiguous with year numbers ("2000" <-> "20:00")
    if (hasHoursEndChar || hasHoursMinutesSeparator) {
        skipWhitespaces(true)
        minutesStr = nextNumberAndAdvance(true, 3)
        if (
            // don't allow a dangling ":"
            minutesStr == null && !hasHoursEndChar ||
            // only allow anything other than 2 digits in minutes if the first digit is a 0
            // (e.g. "030", "0") because e.g. "09:5" can be ambiguous (was "09:50" or "09:05" meant?)
            minutesStr != null && minutesStr.length != 2 && minutesStr.first().digitToInt() != 0
        ) {
            cursor = initial
            return null
        }
    } else {
        minutesStr = null
    }
    // ignore this character ("minutes") after minutes
    if (minutesStr != null) {
        skipWhitespaces(true)
        nextIsAndAdvance('分')
    }
    retreatWhitespaces(true)

    // minutes not considered as missing if "hours end" character has been used. This is important
    // for parsing intervals: Compare "11-18/2" and "11-18/2h". The former 2 should be interpreted
    // as interval in minutes but *could* be understood as hours (which would be wrong, of course),
    // the latter is not ambiguous.
    val minutesStr2 = minutesStr ?: if (hasHoursEndChar) "00" else null

    return Pair(hourStr, minutesStr2)
}

internal fun StringWithCursor.nextIsHoursMinutes(lenient: Boolean): Boolean {
    val initial = cursor
    val hoursMinutes = parseHourMinutes(lenient) ?: return false
    //"Jan 11" should be "January 11th" in lenient mode, not "January 11: o'clock"
    if (hoursMinutes.second == null) {
        cursor = initial
        return false
    }
    // if after hours:minutes there is yet another hours-minutes separator, it wasn't a clock time
    // after all, e.g. "Jan 11:11ː11" (on January 11th at 11:11) or "Jan11:24/7"
    skipWhitespaces(lenient)
    val nextIsSomeTimeElement = nextIsHoursMinutesSeparator(lenient) || nextIs('/')
    cursor = initial // return cursor either way
    return !nextIsSomeTimeElement
}

private fun StringWithCursor.nextIsHoursMinutesSeparator(lenient: Boolean): Boolean =
    if (!lenient) nextIs(':')
    else nextIs { it == ':' || it == '.' || it == '：' || it == 'h' || it == 'H' || it == '時' }


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
