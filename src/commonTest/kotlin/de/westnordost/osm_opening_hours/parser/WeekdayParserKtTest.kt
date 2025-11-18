package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.Weekday
import kotlin.test.Test
import kotlin.test.assertEquals

class WeekdayParserKtTest {

    @Test fun parseWeekday() {
        assertEquals(Weekday.Monday, parseWeekday("Mo"))
        assertEquals(null, parseWeekday("something"))
        assertEquals(null, parseWeekday("mo"))
        assertEquals(null, parseWeekday("Mosaik"))
    }

    @Test fun parseWeekday_lenient() {
        assertEquals(Weekday.Tuesday, parseWeekday("Di", true))
        assertEquals(Weekday.Tuesday, parseWeekday("Di.", true))
        assertEquals(Weekday.Tuesday, parseWeekday("Tuesday", true))
        assertEquals(Weekday.Tuesday, parseWeekday("Tue", true))
        assertEquals(Weekday.Tuesday, parseWeekday("Tue.", true))
        assertEquals(Weekday.Tuesday, parseWeekday("tu", true))
        assertEquals(Weekday.Tuesday, parseWeekday("TU", true))
        assertEquals(Weekday.Tuesday, parseWeekday("TUESDAY", true))

        assertEquals(Weekday.Tuesday, parseWeekday("火", true))
        assertEquals(Weekday.Tuesday, parseWeekday("星期二", true))
        assertEquals(Weekday.Tuesday, parseWeekday("вт", true))
    }

    @Test fun does_not_consume_too_much() {
        verifyConsumption("Mo", false, StringWithCursor::parseWeekday)
        verifyConsumption("Tue.", true, StringWithCursor::parseWeekday)
        verifyConsumption("вт", true, StringWithCursor::parseWeekday)
    }
}

private fun parseWeekday(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseWeekday(lenient)
