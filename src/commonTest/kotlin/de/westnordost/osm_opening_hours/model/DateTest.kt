package de.westnordost.osm_opening_hours.model

import de.westnordost.osm_opening_hours.model.AnnualEvent.Easter
import de.westnordost.osm_opening_hours.model.Month.January
import de.westnordost.osm_opening_hours.model.Month.July
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class CalendarDateTest {
    @Test fun to_string() {
        assertEquals("Jul 22", CalendarDate(July, 22).toString())
        assertEquals("2000 Jul 22", CalendarDate(2000, July, 22).toString())
        assertEquals("Jul 22 +Fr", CalendarDate(July, 22, NextWeekday(Weekday.Friday)).toString())
        assertEquals("Jul 22 -Su", CalendarDate(July, 22, PreviousWeekday(Weekday.Sunday)).toString())
        assertEquals("Jul 22 -Su +3 days", CalendarDate(July, 22, PreviousWeekday(Weekday.Sunday), 3).toString())
    }

    @Test fun bounds() {
        assertFails { CalendarDate(July, 0) }
        assertFails { CalendarDate(July, 32) }
        CalendarDate(July, 31)
        CalendarDate(July, 1)
        assertFails { CalendarDate(1900, July, 22) }
        assertFails { CalendarDate(10000, July, 22) }
        CalendarDate(1901, July, 22)
        CalendarDate(9999, July, 22)
    }
}

class SpecificWeekdayDateTest {
    @Test fun bounds() {
        assertFails { SpecificWeekdayDate(1900, January, Weekday.Friday, Nth(1)) }
        assertFails { SpecificWeekdayDate(10000, January, Weekday.Friday, Nth(1)) }
        SpecificWeekdayDate(1901, January, Weekday.Friday, Nth(1))
        SpecificWeekdayDate(9999, January, Weekday.Friday, Nth(1))
    }

    @Test fun to_string() {
        assertEquals("2000 Jan Fr[1]", SpecificWeekdayDate(2000, January, Weekday.Friday, Nth(1)).toString())
        assertEquals("Jan Fr[1]", SpecificWeekdayDate(January, Weekday.Friday, Nth(1)).toString())
        assertEquals("Jan Fr[1] -2 days", SpecificWeekdayDate(January, Weekday.Friday, Nth(1), -2).toString())
    }
}

class VariableDateTest {
    @Test fun bounds() {
        assertFails { VariableDate(1900, Easter) }
        assertFails { VariableDate(10000, Easter) }
        VariableDate(1901, Easter)
        VariableDate(9999, Easter)
    }

    @Test fun to_string() {
        assertEquals("easter", VariableDate(Easter).toString())
        assertEquals("2000 easter", VariableDate(2000, Easter).toString())
        assertEquals("easter +3 days", VariableDate(Easter, dayOffset = 3).toString())
        assertEquals("easter -1 day", VariableDate(Easter, dayOffset = -1).toString())
        assertEquals(
            "easter +Fr",
            VariableDate(Easter, weekdayOffset = NextWeekday(Weekday.Friday)).toString()
        )
        assertEquals(
            "easter -Su",
            VariableDate(Easter, weekdayOffset = PreviousWeekday(Weekday.Sunday)).toString()
        )
    }
}
