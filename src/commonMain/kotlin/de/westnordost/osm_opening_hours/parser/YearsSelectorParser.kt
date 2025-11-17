package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.StartingAtYear
import de.westnordost.osm_opening_hours.model.Year
import de.westnordost.osm_opening_hours.model.YearRange
import de.westnordost.osm_opening_hours.model.YearsSelector

internal fun StringWithCursor.parseYearsSelector(lenient: Boolean): YearsSelector? {
    val start = nextNumberAndAdvance(lenient, 4) ?: return null
    // not 4 digits -> not a year. Maybe something else, don't throw an exception and return cursor
    if (start.length != 4) {
        retreatBy(start.length)
        return null
    }

    if (nextIsAndAdvance('+', lenient, skipWhitespaces = true)) {
        return StartingAtYear(start.toInt())
    }

    if (nextIsRangeAndAdvance(lenient)) {
        skipWhitespaces(lenient)
        val end = nextNumberAndAdvance(lenient, 4) ?: fail("Expected an end year")
        if (end.length != 4) fail("Expected the end year to consist of 4 digits")

        val step = if (nextIsAndAdvance('/', lenient, skipWhitespaces = true)) {
            skipWhitespaces(lenient)
            nextNumberAndAdvance(lenient)?.toInt() ?: fail("Expected a year interval")
        } else null

        return YearRange(start.toInt(), end.toInt(), step)
    }

    return Year(start.toInt())
}
