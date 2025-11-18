package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.LastNth
import de.westnordost.osm_opening_hours.model.Nth
import de.westnordost.osm_opening_hours.model.NthRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class NthSelectorParserKtTest {
    @Test fun parseNthSelector() {
        assertEquals(LastNth(3), parseNthSelector("-3"))
        assertEquals(LastNth(3), parseNthSelector("- 3"))
        assertEquals(Nth(1), parseNthSelector("1"))
        assertEquals(NthRange(1, 4), parseNthSelector("1-4"))
        assertEquals(NthRange(1, 4), parseNthSelector("1  - 4"))

        assertEquals(Nth(1), parseNthSelector("1—3"))

        assertFails { parseNthSelector("-3-4") }
        assertFails { parseNthSelector("3-") }
        assertFails { parseNthSelector("3-1") }
    }

    @Test fun parseNthSelector_lenient() {
        assertEquals(NthRange(1, 3), parseNthSelector("1—3", true))
        assertEquals(NthRange(1, 3), parseNthSelector("１〜３", true))
    }

    @Test fun does_not_consume_too_much() {
        verifyConsumption("1", false, StringWithCursor::parseNthSelector)
        verifyConsumption("1-2", false, StringWithCursor::parseNthSelector)
        verifyConsumption("-4", false, StringWithCursor::parseNthSelector)
    }
}

private fun parseNthSelector(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseNthSelector(lenient)
