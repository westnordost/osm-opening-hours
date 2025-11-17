package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.Week
import de.westnordost.osm_opening_hours.model.WeekRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class WeeksSelectorParserKtTest {

    @Test fun parseWeeksSelector() {
        assertEquals(Week(1), parseWeeksSelector("01"))
        assertEquals(Week(22), parseWeeksSelector("22"))
        assertEquals(Week(3), parseWeeksSelector("03"))

        assertEquals(WeekRange(10, 20), parseWeeksSelector("10-20"))
        assertEquals(WeekRange(10, 20), parseWeeksSelector("10 - 20"))

        assertEquals(WeekRange(10, 20, 2), parseWeeksSelector("10-20/2"))
        assertEquals(WeekRange(10, 20, 2), parseWeeksSelector("10 - 20  / 2"))

        assertEquals(Week(1), parseWeeksSelector("01—05"))

        assertFails { parseWeeksSelector("00") }
        assertFails { parseWeeksSelector("1") }
        assertFails { parseWeeksSelector("01-5") }
        assertFails { parseWeeksSelector("01-5") }
        assertFails { parseWeeksSelector("54") }
    }

    @Test fun parseWeeksSelector_lenient() {
        assertEquals(Week(1), parseWeeksSelector("1", true))
        assertEquals(WeekRange(1,5), parseWeeksSelector("01-5", true))
        assertEquals(WeekRange(1,5), parseWeeksSelector("01—05", true))
        assertEquals(WeekRange(1,5), parseWeeksSelector("01〜05", true))
        assertEquals(WeekRange(1,5), parseWeeksSelector("01 — 05", true))
        assertEquals(WeekRange(1,5), parseWeeksSelector("01 to 05", true))

        assertEquals(Week(12), parseWeeksSelector("١٢", true))
        assertEquals(Week(12), parseWeeksSelector("１２", true))
        assertEquals(Week(12), parseWeeksSelector("๑๒", true))
    }

    @Test
    fun does_not_consume_too_much() {
        verifyConsumption("24", false, StringWithCursor::parseWeeksSelector)
        verifyConsumption("12-24", false, StringWithCursor::parseWeeksSelector)
        verifyConsumption("12-24/3", false, StringWithCursor::parseWeeksSelector)
    }
}

// convenience shortcuts
private fun parseWeeksSelector(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseWeeksSelector(lenient)
