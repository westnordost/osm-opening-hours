package de.westnordost.osm_opening_hours.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import de.westnordost.osm_opening_hours.model.Weekday.*

class WeekdayTest {
    @Test fun rangeTo() {
        assertEquals(
            WeekdayRange(Monday, Monday),
            Monday..Monday
        )
        assertEquals(
            WeekdayRange(Monday, Friday),
            Monday..Friday
        )
    }
}

class WeekdayRangeTest {
    @Test fun toWeekdays() {
        assertEquals(
            listOf(Monday),
            (Monday..Monday).toWeekdays()
        )
        assertEquals(
            listOf(Monday, Tuesday, Wednesday, Thursday, Friday),
            (Monday..Friday).toWeekdays()
        )
        assertEquals(
            listOf(Sunday, Monday),
            (Sunday..Monday).toWeekdays()
        )
    }
}

class SpecificWeekdaysTest {

    @Test fun bounds() {
        assertFails { SpecificWeekdays(Monday, listOf()) }
    }

    @Test fun to_string() {
        assertEquals("Mo[3]", SpecificWeekdays(Monday, listOf(Nth(3))).toString())
        assertEquals("Tu[-2]", SpecificWeekdays(Tuesday, listOf(LastNth(2))).toString())
        assertEquals("We[1-3]", SpecificWeekdays(Wednesday, listOf(NthRange(1,3))).toString())

        assertEquals("Mo[3] +3 days", SpecificWeekdays(Monday, listOf(Nth(3)), 3).toString())
    }
}

