package de.westnordost.osm_opening_hours.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

import de.westnordost.osm_opening_hours.model.EventTime.*

class ClockTimeTest {
    @Test fun bounds() {
        assertFails { ClockTime(-1, 0) }
        assertFails { ClockTime(24, 0) }
        assertFails { ClockTime(0, -1) }
        assertFails { ClockTime(0, 60) }
        ClockTime(0, 0)
        ClockTime(23, 59)
    }

    @Test fun to_string() {
        assertEquals("00:00", ClockTime(0, 0).toString())
        assertEquals("10:10", ClockTime(10, 10).toString())
    }
}

class ExtendedClockTimeTest {
    @Test fun bounds() {
        assertFails { ExtendedClockTime(-1, 0) }
        assertFails { ExtendedClockTime(48, 0) }
        assertFails { ExtendedClockTime(0, -1) }
        assertFails { ExtendedClockTime(0, 60) }
        ExtendedClockTime(0, 0)
        ExtendedClockTime(47, 59)
    }

    @Test fun to_string() {
        assertEquals("00:00", ExtendedClockTime(0, 0).toString())
        assertEquals("40:10", ExtendedClockTime(40, 10).toString())
    }
}

class EventTimeTest {
    @Test fun to_string() {
        assertEquals("dawn", Dawn.toString())
        assertEquals("sunrise", Sunrise.toString())
        assertEquals("sunset", Sunset.toString())
        assertEquals("dusk", Dusk.toString())
    }

    @Test fun operators() {
        assertEquals(
            VariableTime(Dawn, TimeOffset(OffsetOp.Plus, ClockTime(1, 0))),
            Dawn + ClockTime(1, 0)
        )

        assertEquals(
            VariableTime(Sunset, TimeOffset(OffsetOp.Minus, ClockTime(0, 30))),
            Sunset - ClockTime(0, 30)
        )
    }
}

class EventTimeWithOffsetTest {
    @Test fun to_string() {
        assertEquals("(dawn+01:00)", (Dawn + ClockTime(1, 0)).toString())
        assertEquals("(sunset-00:30)", (Sunset - ClockTime(0, 30)).toString())
    }
}

class TimeIntervalsTest {
    @Test fun to_string() {
        assertEquals(
            "00:30-12:00/90",
            TimeIntervals(ClockTime(0, 30), ClockTime(12, 0), IntervalMinutes(90)).toString()
        )
        assertEquals(
            "20:00-05:05/5",
            TimeIntervals(ClockTime(20, 0), ClockTime(5, 5), IntervalMinutes(5)).toString()
        )
        assertEquals(
            "sunrise-12:00/01:30",
            TimeIntervals(VariableTime(Sunrise), ClockTime(12, 0), ClockTime(1, 30)).toString()
        )
        assertEquals(
            "(sunrise+01:00)-(sunset-01:00)/01:30",
            TimeIntervals(
                Sunrise + ClockTime(1, 0),
                Sunset - ClockTime(1, 0),
                ClockTime(1, 30)
            ).toString()
        )
    }
}

class IntervalMinutesTest {
    @Test fun bounds() {
        assertFails { IntervalMinutes(0) }
        IntervalMinutes(1)
    }
}

class TimeSpanTest {
    @Test fun to_string() {
        assertEquals(
            "00:30-12:00",
            TimeSpan(ClockTime(0, 30), ClockTime(12, 0)).toString()
        )
        assertEquals(
            "20:00-05:05",
            TimeSpan(ClockTime(20, 0), ClockTime(5, 5)).toString()
        )
        assertEquals(
            "00:00-00:00",
            TimeSpan(ClockTime(0), ExtendedClockTime(0)).toString()
        )
        assertEquals(
            "00:30-24:00",
            TimeSpan(ClockTime(0, 30), ExtendedClockTime(0, 0)).toString()
        )
        assertEquals(
            "00:30-24:00",
            TimeSpan(ClockTime(0, 30), ExtendedClockTime(24, 0)).toString()
        )
        assertEquals(
            "20:00-19:59",
            TimeSpan(ClockTime(20, 0), ExtendedClockTime(43, 59)).toString()
        )
        assertEquals(
            "20:00-44:00",
            TimeSpan(ClockTime(20, 0), ExtendedClockTime(44, 0)).toString()
        )
        assertEquals(
            "20:30-20:29",
            TimeSpan(ClockTime(20, 30), ExtendedClockTime(44, 29)).toString()
        )
        assertEquals(
            "20:30-44:30",
            TimeSpan(ClockTime(20, 30), ExtendedClockTime(44, 30)).toString()
        )
        assertEquals(
            "sunrise-12:00+",
            TimeSpan(VariableTime(Sunrise), ClockTime(12, 0), openEnd = true).toString()
        )
        assertEquals(
            "(sunrise+01:00)-(sunset-01:00)+",
            TimeSpan(
                Sunrise + ClockTime(1, 0),
                Sunset - ClockTime(1, 0),
                openEnd = true
            ).toString()
        )
    }
}

class StartingAtTimeTest {
    @Test fun to_string() {
        assertEquals("10:00+", StartingAtTime(ClockTime(10, 0)).toString())
        assertEquals("dawn+", StartingAtTime(VariableTime(Dawn)).toString())
        assertEquals("(dawn-00:30)+", StartingAtTime(Dawn - ClockTime(0, 30)).toString())
    }
}
