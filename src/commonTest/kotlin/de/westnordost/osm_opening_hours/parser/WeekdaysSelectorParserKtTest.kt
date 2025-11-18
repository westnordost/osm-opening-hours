package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class WeekdaysSelectorParserKtTest {

    @Test fun parseWeekdaySelector() {
        assertEquals(Weekday.Monday, parseWeekdaySelector("Mo"))
        assertEquals(Weekday.Monday..Weekday.Thursday, parseWeekdaySelector("Mo-Th"))
        assertEquals(Weekday.Monday..Weekday.Thursday, parseWeekdaySelector("Mo - Th"))
        assertEquals(Weekday.Monday..Weekday.Thursday, parseWeekdaySelector("Mo  -  Th"))
        assertEquals(SpecificWeekdays(Weekday.Monday, listOf(Nth(1))), parseWeekdaySelector("Mo[1]"))
        assertEquals(SpecificWeekdays(Weekday.Monday, listOf(Nth(1))), parseWeekdaySelector("Mo [ 1 ]"))
        assertEquals(
            SpecificWeekdays(Weekday.Monday, listOf(Nth(1), NthRange(2,3), LastNth(1))),
            parseWeekdaySelector("Mo[1,2-3,-1]")
        )
        assertEquals(
            SpecificWeekdays(Weekday.Monday, listOf(Nth(1), NthRange(2,3), LastNth(1))),
            parseWeekdaySelector("Mo[ 1  ,  2-3  , -1  ]")
        )
        assertEquals(SpecificWeekdays(Weekday.Monday, listOf(Nth(1)), 1), parseWeekdaySelector("Mo[1] +1 day"))
        assertEquals(null, parseWeekdaySelector("something"))

        assertEquals(Weekday.Thursday, parseWeekdaySelector("Th—Fr"))
        assertEquals(Weekday.Thursday, parseWeekdaySelector("Th.-Fr."))
        assertFails { parseWeekdaySelector("Mo-") }
        assertFails { parseWeekdaySelector("Mo[") }
        assertFails { parseWeekdaySelector("Mo[]") }
        assertFails { parseWeekdaySelector("Mo[1,]") }
        assertFails { parseWeekdaySelector("Mo[6]") }
        assertFails { parseWeekdaySelector("Mo[-6]") }
    }

    @Test fun parseWeekdaySelector_lenient() {
        assertEquals(
            SpecificWeekdays(Weekday.Thursday, listOf(Nth(1)), 1),
            parseWeekdaySelector("DO[1]+1DAY", true)
        )
        assertEquals(Weekday.Thursday..Weekday.Friday, parseWeekdaySelector("Th—Fr", true))
        assertEquals(Weekday.Thursday..Weekday.Friday, parseWeekdaySelector("Th.-Fr.", true))
        assertEquals(Weekday.Thursday..Weekday.Friday, parseWeekdaySelector("Th — Fr", true))
        assertEquals(Weekday.Thursday..Weekday.Friday, parseWeekdaySelector("Th〜Fr", true))
        assertEquals(Weekday.Thursday..Weekday.Friday, parseWeekdaySelector("Th to Fr", true))
    }

    @Test fun parseDayOffset() {
        assertEquals(3, parseDayOffset("+3 days"))
        assertEquals(3, parseDayOffset("+3days"))
        assertEquals(-2, parseDayOffset("-  2 day"))
        assertEquals(0, parseDayOffset("+ 0   day"))
        assertEquals(null, parseDayOffset("something else"))

        assertEquals(null, parseDayOffset("+"))
        assertEquals(null, parseDayOffset("-"))
        assertEquals(null, parseDayOffset("+3"))
    }

    @Test fun parseDayOffset_lenient() {
        assertEquals(3, parseDayOffset("+3DAYS", true))
    }

    @Test fun does_not_consume_too_much() {
        verifyConsumption("Mo", false, StringWithCursor::parseWeekdaySelector)
        verifyConsumption("Mo-Sa", false, StringWithCursor::parseWeekdaySelector)
        verifyConsumption("Mo[3]", false, StringWithCursor::parseWeekdaySelector)
        verifyConsumption("Mo[3] +3 days", false, StringWithCursor::parseWeekdaySelector)

        verifyConsumption(" +3 days", false, StringWithCursor::parseDayOffset)
    }
}

private fun parseWeekdaySelector(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseWeekdaySelector(lenient)

private fun parseDayOffset(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseDayOffset(lenient)

