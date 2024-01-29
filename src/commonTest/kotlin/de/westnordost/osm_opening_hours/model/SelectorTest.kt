package de.westnordost.osm_opening_hours.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue
import kotlin.test.assertFalse

import de.westnordost.osm_opening_hours.model.Weekday.*
import de.westnordost.osm_opening_hours.model.Holiday.*
import de.westnordost.osm_opening_hours.model.Month.*

class TwentyFourSevenTest {
    @Test fun containsTimePoints() {
        assertEquals(false, TwentyFourSeven.containsTimePoints())
    }

    @Test fun containsTimeSpans() {
        assertEquals(true, TwentyFourSeven.containsTimeSpans())
    }

    @Test fun to_string() {
        assertEquals("24/7", TwentyFourSeven.toString())
    }

    @Test fun is_empty() {
        assertFalse(TwentyFourSeven.isEmpty())
    }
}

class RangeTest {
    @Test fun containsTimePoints() {
        assertEquals(false, Range().containsTimePoints())
        assertEquals(
            false,
            Range(times = listOf(ClockTime(8)..ClockTime(9))).containsTimePoints()
        )
        assertEquals(
            true,
            Range(times = listOf(ClockTime(8))).containsTimePoints()
        )
        assertEquals(
            true,
            Range(times = listOf(ClockTime(8)..ClockTime(9), ClockTime(10))).containsTimePoints()
        )
    }

    @Test fun containsTimeSpans() {
        assertEquals(false, Range().containsTimeSpans())
        assertEquals(
            true,
            Range(times = listOf(ClockTime(8)..ClockTime(9))).containsTimeSpans()
        )
        assertEquals(
            false,
            Range(times = listOf(ClockTime(8))).containsTimeSpans()
        )
        assertEquals(
            true,
            Range(times = listOf(ClockTime(8)..ClockTime(9), ClockTime(10))).containsTimeSpans()
        )
    }

    @Test fun to_string() {
        assertEquals("", Range().toString())

        assertEquals("Mo", Range(weekdays = listOf(Monday)).toString())
        assertEquals("Mo,Tu", Range(weekdays = listOf(Monday, Tuesday)).toString())
        assertEquals("Tu,Mo", Range(weekdays = listOf(Tuesday, Monday)).toString())

        assertEquals("PH", Range(holidays = listOf(PublicHoliday)).toString())
        assertEquals("PH,SH", Range(holidays = listOf(PublicHoliday, SchoolHoliday)).toString())
        assertEquals("SH,PH", Range(holidays = listOf(SchoolHoliday, PublicHoliday)).toString())

        assertEquals("Mo,PH", Range(
            weekdays = listOf(Monday),
            holidays = listOf(PublicHoliday)
        ).toString())

        assertEquals("Sa,Su,SH,PH",
            Range(
                weekdays = listOf(Saturday, Sunday),
                holidays = listOf(SchoolHoliday, PublicHoliday)
            ).toString()
        )

        assertEquals("PH Mo", Range(
            weekdays = listOf(Monday),
            holidays = listOf(PublicHoliday),
            isRestrictedByHolidays = true
        ).toString())

        assertEquals("PH,SH Mo,Tu",
            Range(
                weekdays = listOf(Monday, Tuesday),
                holidays = listOf(PublicHoliday, SchoolHoliday),
                isRestrictedByHolidays = true
            ).toString()
        )

        assertEquals("10:20", Range(times = listOf(ClockTime(10, 20))).toString())

        assertEquals("Mo 10:20", Range(listOf(Monday), listOf(ClockTime(10, 20))).toString())

        assertEquals("1999", Range(years = listOf(Year(1999))).toString())

        assertEquals("Jan", Range(months = listOf(SingleMonth(January))).toString())

        assertEquals("week 05", Range(weeks = listOf(Week(5))).toString())

        assertEquals("Jan: Mo 10:20",
            Range(
                months = listOf(SingleMonth(January)),
                weekdays = listOf(Monday),
                times = listOf(ClockTime(10, 20))
            ).toString()
        )

        assertEquals("Jan Mo 10:20",
            Range(
                months = listOf(SingleMonth(January)),
                useSeparatorForReadability = false,
                weekdays = listOf(Monday),
                times = listOf(ClockTime(10, 20))
            ).toString()
        )

        assertEquals("1999: Mo 10:20",
            Range(
                years = listOf(Year(1999)),
                weekdays = listOf(Monday),
                times = listOf(ClockTime(10, 20))
            ).toString()
        )

        assertEquals("week 05: Mo 10:20",
            Range(
                weeks = listOf(Week(5)),
                weekdays = listOf(Monday),
                times = listOf(ClockTime(10, 20))
            ).toString()
        )

        assertEquals("1999 Jan: Mo 10:20",
            Range(
                years = listOf(Year(1999)),
                months = listOf(SingleMonth(January)),
                weekdays = listOf(Monday),
                times = listOf(ClockTime(10, 20))
            ).toString()
        )

        assertEquals("1999,2000 Jan,Feb week 05,10: Mo 10:20",
            Range(
                years = listOf(Year(1999), Year(2000)),
                months = listOf(SingleMonth(January), SingleMonth(February)),
                weeks = listOf(Week(5), Week(10)),
                weekdays = listOf(Monday),
                times = listOf(ClockTime(10, 20))
            ).toString()
        )

        assertEquals(
            "\"In the raining season\":",
            Range(text = "In the raining season").toString()
        )

        assertEquals(
            "\"a\": Mo",
            Range(text = "a", weekdays = listOf(Monday)).toString()
        )
    }


    @Test fun bounds() {
        assertFails { Range(text = "In the \"raining season\"") }
        assertFails { Range(text = "Yeah", years = listOf(Year(1999))) }
    }

    @Test fun is_empty() {
        assertTrue(Range().isEmpty())
        assertTrue(Range(years = listOf()).isEmpty())
        assertTrue(Range(months = listOf()).isEmpty())
        assertTrue(Range(weeks = listOf()).isEmpty())
        assertTrue(Range(weekdays = listOf()).isEmpty())
        assertTrue(Range(holidays = listOf()).isEmpty())
        assertTrue(Range(times = listOf()).isEmpty())

        assertFalse(Range(text = "a").isEmpty())
        assertFalse(Range(years = listOf(Year(1999))).isEmpty())
        assertFalse(Range(months = listOf(SingleMonth(January))).isEmpty())
        assertFalse(Range(weeks = listOf(Week(1))).isEmpty())
        assertFalse(Range(weekdays = listOf(Monday)).isEmpty())
        assertFalse(Range(holidays = listOf(PublicHoliday)).isEmpty())
        assertFalse(Range(times = listOf(ClockTime(1))).isEmpty())
    }
}
