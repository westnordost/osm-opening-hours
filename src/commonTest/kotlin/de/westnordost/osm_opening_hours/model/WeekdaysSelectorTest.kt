package de.westnordost.osm_opening_hours.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class SpecificWeekdaysTest {

    @Test fun bounds() {
        assertFails { SpecificWeekdays(Weekday.Monday, listOf()) }
    }

    @Test fun to_string() {
        assertEquals("Mo[3]", SpecificWeekdays(Weekday.Monday, listOf(Nth(3))).toString())
        assertEquals("Tu[-2]", SpecificWeekdays(Weekday.Tuesday, listOf(LastNth(2))).toString())
        assertEquals("We[1-3]", SpecificWeekdays(Weekday.Wednesday, listOf(NthRange(1,3))).toString())

        assertEquals("Mo[3] +3 days", SpecificWeekdays(Weekday.Monday, listOf(Nth(3)), 3).toString())
    }
}

