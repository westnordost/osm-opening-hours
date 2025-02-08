package de.westnordost.osm_opening_hours.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

import de.westnordost.osm_opening_hours.model.Month.*
import de.westnordost.osm_opening_hours.model.AnnualEvent.*

class MonthTest {
    @Test fun operators() {
        assertEquals(MonthRange(April, August), April..August)
    }

    @Test fun to_string() {
        assertEquals("Jan", January.toString())
    }
}

class MonthRangeTest {
    @Test fun bounds() {
        assertFails { MonthRange(1900, July, January) }
        assertFails { MonthRange(10000, July, January) }
        MonthRange(1901, July, January)
        MonthRange(9999, July, January)
    }

    @Test fun to_string() {
        assertEquals("Dec-Jan", MonthRange(December, January).toString())
        assertEquals("2000 Dec-Jan", MonthRange(2000, December, January).toString())
    }
}

class MonthInYearTest {
    @Test fun bounds() {
        assertFails { SingleMonth(1900, July) }
        assertFails { SingleMonth(10000, July) }
        SingleMonth(1901, July)
        SingleMonth(9999, July)
    }

    @Test fun to_string() {
        assertEquals("2000 Dec", SingleMonth(2000, December).toString())
    }
}

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

class StartingAtDateTest {
    @Test fun to_string() {
        assertEquals("Mar 12+", StartingAtDate(CalendarDate(March, 12)).toString())
        // From the saturday after March 12 onwards
        assertEquals(
            "Mar 12 +Sa+", // lol wut
            StartingAtDate(CalendarDate(March, 12, NextWeekday(Weekday.Saturday))).toString()
        )
    }
}

class DateRangeTest {
    @Test fun to_string() {
        assertEquals(
            "easter-Feb 02",
            (VariableDate(Easter)..CalendarDate(February, 2)).toString()
        )
        // 3 days before Easter till the Sunday after the first of September %-)
        assertEquals(
            "easter -1 day-Sep 01 +Su",
            DateRange(
                VariableDate(Easter, dayOffset = -1),
                CalendarDate(September, 1, NextWeekday(Weekday.Sunday))
            ).toString()
        )
    }
}

class DatesInMonthTest {
    @Test fun bounds() {
        assertFails { DatesInMonth(1900, July, MonthDay(1)) }
        assertFails { DatesInMonth(10000, July, MonthDay(1)) }
        DatesInMonth(1901, July, MonthDay(1))
        DatesInMonth(9999, July, MonthDay(1))
    }

    @Test fun to_string() {
        assertEquals(
            "Jul 02",
            DatesInMonth(July, MonthDay(2)).toString()
        )
        assertEquals(
            "Jul 02-09",
            DatesInMonth(July, MonthDayRange(2, 9)).toString()
        )
        assertEquals(
            "2000 Jul 02",
            DatesInMonth(2000, July, MonthDay(2)).toString()
        )

        assertEquals(
            "Jul 02-09,Jul 01",
            DatesInMonth(July, MonthDayRange(2, 9), MonthDay(1)).toString()
        )
        assertEquals(
            "2000 Jul 02-09,2000 Jul 01",
            DatesInMonth(2000, July, MonthDayRange(2, 9), MonthDay(1)).toString()
        )
    }
}

class MonthDayTest {
    @Test fun bounds() {
        assertFails { MonthDay(0) }
        assertFails { MonthDay(32) }
        MonthDay(1)
        MonthDay(31)
    }

    @Test fun to_string() {
        assertEquals("01", MonthDay(1).toString())
        assertEquals("10", MonthDay(10).toString())
    }
}

class MonthDayRangeTest {
    @Test fun bounds() {
        assertFails { MonthDayRange(0, 1) }
        assertFails { MonthDayRange(30, 32) }
        assertFails { MonthDayRange(31, 31) }
        assertFails { MonthDayRange(1, 0) }
        assertFails { MonthDayRange(20, 20) }
        MonthDayRange(1, 31)
        MonthDayRange(1, 2)
        MonthDayRange(30, 31)
    }

    @Test fun to_string() {
        assertEquals("01-02", MonthDayRange(1,2).toString())
        assertEquals("10-20", MonthDayRange(10,20).toString())
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