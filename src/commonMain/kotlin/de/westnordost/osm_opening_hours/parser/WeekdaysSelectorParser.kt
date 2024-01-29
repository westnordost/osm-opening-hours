package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.*
import de.westnordost.osm_opening_hours.model.Weekday.*

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
    val days = nextNumberAndAdvance()
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

internal fun StringWithCursor.parseNthSelector(lenient: Boolean): NthSelector {
    val minus = nextIsAndAdvance('-')

    skipWhitespaces(lenient)

    val start = nextNumberAndAdvance(1)?.toInt() ?: fail("Expected an nth")

    val end = if (nextIsRangeAndAdvance(lenient)) {
        if (minus) fail("Negative nth not allowed in range")
        skipWhitespaces(lenient)
        nextNumberAndAdvance(1)?.toInt() ?: fail("Expected an end nth")
    } else null

    return when {
        minus ->       LastNth(start)
        end != null -> NthRange(start, end)
        else ->        Nth(start)
    }
}

internal fun StringWithCursor.parseWeekday(lenient: Boolean): Weekday? {
    return if (lenient) parseWeekdayLenient() else parseWeekdayStrict()
}

private val weekdaysMap: Map<String, Weekday> = Weekday.entries.associateBy { it.osm }
private val weekdaysMaxLength: Int = weekdaysMap.keys.maxOf { it.length }

private fun StringWithCursor.parseWeekdayStrict(): Weekday? {
    val word = getNextKeyword(weekdaysMaxLength) ?: return null
    val event = weekdaysMap[word] ?: return null
    advanceBy(word.length)
    return event
}

private val lenientWeekdaysMap: Map<String, Weekday> = (
    // correct 2-letter abbreviations
    Weekday.entries.associateBy { it.osm } +
    // full names
    Weekday.entries.associateBy { it.name } +
    // three-letter abbreviations
    mapOf(
        "Mon" to Monday,
        "Tue" to Tuesday,
        "Wed" to Wednesday,
        "Thu" to Thursday,
        "Fri" to Friday,
        "Sat" to Saturday,
        "Sun" to Sunday,
    ) +
    // German 2-letter abbreviations (a common mistake)
    mapOf(
        "Mo" to Monday,
        "Di" to Tuesday,
        "Mi" to Wednesday,
        "Do" to Thursday,
        "Fr" to Friday,
        "Sa" to Saturday,
        "So" to Sunday,
    )
).mapKeys { it.key.lowercase() }
private val lenientWeekdaysMaxLength: Int = lenientWeekdaysMap.keys.maxOf { it.length }

private fun StringWithCursor.parseWeekdayLenient(): Weekday? {
    val word = getNextKeyword(lenientWeekdaysMaxLength)?.lowercase() ?: return null
    val event = lenientWeekdaysMap[word] ?: return null
    advanceBy(word.length)
    nextIsAndAdvance('.')
    return event
}
