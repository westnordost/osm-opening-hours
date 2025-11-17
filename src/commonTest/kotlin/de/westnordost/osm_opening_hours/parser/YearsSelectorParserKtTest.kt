package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.StartingAtYear
import de.westnordost.osm_opening_hours.model.Year
import de.westnordost.osm_opening_hours.model.YearRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class YearsSelectorParserKtTest {

    @Test fun parseYearsSelector() {
        assertEquals(Year(1999), parseYearsSelector("1999"))
        assertEquals(Year(9999), parseYearsSelector("9999"))
        assertEquals(StartingAtYear(1999), parseYearsSelector("1999+"))
        assertEquals(StartingAtYear(1999), parseYearsSelector("1999  +"))
        assertEquals(YearRange(1901, 2000), parseYearsSelector("1901-2000"))
        assertEquals(YearRange(1901, 2000), parseYearsSelector("1901 - 2000"))
        assertEquals(YearRange(1901, 2000, 3), parseYearsSelector("1901-2000/3"))
        assertEquals(YearRange(1901, 2000, 3), parseYearsSelector("1901-2000 / 3"))

        assertEquals(null, parseYearsSelector("99999"))
        assertEquals(null, parseYearsSelector("999"))
        assertEquals(null, parseYearsSelector("something"))
        assertEquals(Year(1999), parseYearsSelector("1999—2000"))

        assertFails { parseYearsSelector("1895") }
        assertFails { parseYearsSelector("1901-") }
        assertFails { parseYearsSelector("1901-2000/") }
        assertFails { parseYearsSelector("1901-20000") }
        assertFails { parseYearsSelector("1901-200") }
        assertFails { parseYearsSelector("3000-2000") }
    }

    @Test fun parseYearsSelector_lenient() {
        assertEquals(YearRange(1999, 2000), parseYearsSelector("1999—2000", true))
        assertEquals(YearRange(1999, 2000), parseYearsSelector("1999〜2000", true))
        assertEquals(YearRange(1999, 2000), parseYearsSelector("1999 — 2000", true))
        assertEquals(YearRange(1999, 2000), parseYearsSelector("1999 to 2000", true))
    }

    @Test fun does_not_consume_too_much() {
        verifyConsumption("1901", false, StringWithCursor::parseYearsSelector)
        verifyConsumption("1901+", false, StringWithCursor::parseYearsSelector)
        verifyConsumption("1901-2000", false, StringWithCursor::parseYearsSelector)
        verifyConsumption("1901-2000/3", false, StringWithCursor::parseYearsSelector)

        verifyConsumption("something", false, expectCursorPosAt = 0, StringWithCursor::parseYearsSelector)
        verifyConsumption("10", false, expectCursorPosAt = 0, StringWithCursor::parseYearsSelector)
    }
}

// convenience shortcuts
private fun parseYearsSelector(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseYearsSelector(lenient)
