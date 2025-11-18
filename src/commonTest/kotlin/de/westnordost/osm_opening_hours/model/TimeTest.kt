package de.westnordost.osm_opening_hours.model

import de.westnordost.osm_opening_hours.model.EventTime.Dawn
import de.westnordost.osm_opening_hours.model.EventTime.Dusk
import de.westnordost.osm_opening_hours.model.EventTime.Sunrise
import de.westnordost.osm_opening_hours.model.EventTime.Sunset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ClockTimeTest {
    @Test fun bounds() {
        assertFails { ClockTime(-1, 0) }
        assertFails { ClockTime(24, 1) }
        assertFails { ClockTime(0, -1) }
        assertFails { ClockTime(0, 60) }
        ClockTime(0, 0)
        ClockTime(23, 59)
        ClockTime(24, 0)
    }

    @Test fun to_string() {
        assertEquals("00:00", ClockTime(0, 0).toString())
        assertEquals("10:10", ClockTime(10, 10).toString())
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

class VariableTimeTest {
    @Test fun to_string() {
        assertEquals("(dawn+01:00)", (Dawn + ClockTime(1, 0)).toString())
        assertEquals("(sunset-00:30)", (Sunset - ClockTime(0, 30)).toString())
    }
}

class ExtendedClockTimeTest {
    @Test fun bounds() {
        assertFails { ExtendedClockTime(-1, 0) }
        assertFails { ExtendedClockTime(48, 1) }
        assertFails { ExtendedClockTime(0, -1) }
        assertFails { ExtendedClockTime(0, 60) }
        ExtendedClockTime(0, 0)
        ExtendedClockTime(47, 59)
        ExtendedClockTime(48, 0)
    }

    @Test fun to_string() {
        assertEquals("00:00", ExtendedClockTime(0, 0).toString())
        assertEquals("40:10", ExtendedClockTime(40, 10).toString())
    }
}
