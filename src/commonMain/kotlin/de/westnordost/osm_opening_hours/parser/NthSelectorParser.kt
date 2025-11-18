package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.LastNth
import de.westnordost.osm_opening_hours.model.Nth
import de.westnordost.osm_opening_hours.model.NthRange
import de.westnordost.osm_opening_hours.model.NthSelector


internal fun StringWithCursor.parseNthSelector(lenient: Boolean): NthSelector {
    val minus = nextIsAndAdvance('-')

    skipWhitespaces(lenient)

    val start = nextNumberAndAdvance(lenient, 1)?.toInt() ?: fail("Expected an nth")

    val end = if (nextIsRangeAndAdvance(lenient)) {
        if (minus) fail("Negative nth not allowed in range")
        skipWhitespaces(lenient)
        nextNumberAndAdvance(lenient, 1)?.toInt() ?: fail("Expected an end nth")
    } else null

    return when {
        minus ->       LastNth(start)
        end != null -> NthRange(start, end)
        else ->        Nth(start)
    }
}
