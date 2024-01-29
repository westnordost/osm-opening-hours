package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.Week
import de.westnordost.osm_opening_hours.model.WeekRange
import de.westnordost.osm_opening_hours.model.WeeksSelector

internal fun StringWithCursor.parseWeeksSelector(lenient: Boolean): WeeksSelector {
    val start = nextNumberAndAdvance(2) ?: fail("Expected a week number")
    if (!lenient && start.length != 2) fail("Expected week number to consist of two digits")

    if (nextIsRangeAndAdvance(lenient)) {
        skipWhitespaces(lenient)
        val end = nextNumberAndAdvance(2) ?: fail("Expected an end week number")
        if (!lenient && end.length != 2) fail("Expected end week number to consist of two digits")

        val step = if (nextIsAndAdvance('/', lenient, skipWhitespaces = true)) {
            skipWhitespaces(lenient)
            nextNumberAndAdvance()?.toInt() ?: fail("Expected a week interval")
        } else null

        return WeekRange(start.toInt(), end.toInt(), step)
    }

    return Week(start.toInt())
}
