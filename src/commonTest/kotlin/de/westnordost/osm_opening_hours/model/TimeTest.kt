package de.westnordost.osm_opening_hours.model

import de.westnordost.osm_opening_hours.model.EventTime.Dawn
import de.westnordost.osm_opening_hours.model.EventTime.Dusk
import de.westnordost.osm_opening_hours.model.EventTime.Sunrise
import de.westnordost.osm_opening_hours.model.EventTime.Sunset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class ClockTimeTest {
    @Test fun bounds() {
        assertFails { ClockTime(-1, 0) }
        assertFails { ClockTime(0, -1) }
        assertFails { ClockTime(0, 60) }
        ClockTime(0, 0)
        ClockTime(23, 59)
        ClockTime(24, 0)
        ClockTime(24, 59)
    }

    @Test fun toMinutesOfDay() {
        assertEquals(
            12 * 60 + 59,
            ClockTime(12,59).toMinutesOfDay()
        )
    }

    @Test fun compareTo() {
        assertEquals(0, ClockTime(15,40).compareTo(ClockTime(15,40)))
        assertTrue(ClockTime(15,39) < ClockTime(15,40))
        assertTrue(ClockTime(14,41) < ClockTime(15,40))
        assertTrue(ClockTime(15,40) > ClockTime(15,39))
        assertTrue(ClockTime(15,40) > ClockTime(14,41))
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

    @Test fun toMinutesOfDay() {
        assertEquals(
            6 * 60,
            Sunrise.toMinutesOfDay()
        )
        assertEquals(
            18 * 60,
            Sunset.toMinutesOfDay()
        )
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

    @Test fun toMinutesOfDay() {
        assertEquals(
            Dawn.toMinutesOfDay() + 90,
            (Dawn + ClockTime(1, 30)).toMinutesOfDay()
        )

        assertEquals(
            Dawn.toMinutesOfDay() - 30,
            (Dawn - ClockTime(0, 30)).toMinutesOfDay()
        )

        assertEquals(
            Dawn.toMinutesOfDay(),
            VariableTime(Dawn).toMinutesOfDay()
        )
    }

    @Test fun compareTo() {
        assertEquals(0, VariableTime(Dawn).compareTo(VariableTime(Dawn)))
        assertEquals(0, (Dawn - ClockTime(0, 30)).compareTo((Dawn - ClockTime(0, 30))))

        assertTrue(VariableTime(Dawn) < VariableTime(Sunrise))
        assertTrue(VariableTime(Sunrise) > VariableTime(Dawn))
    }
}

class ExtendedClockTimeTest {
    @Test fun bounds() {
        assertFails { ExtendedClockTime(-1, 0) }
        assertFails { ExtendedClockTime(0, -1) }
        assertFails { ExtendedClockTime(0, 60) }
        ExtendedClockTime(0, 0)
        ExtendedClockTime(47, 59)
        ExtendedClockTime(48, 0)
        ExtendedClockTime(48, 59)
    }

    @Test fun toMinutesOfDay() {
        assertEquals(
            47 * 60 + 59,
            ExtendedClockTime(47,59).toMinutesOfDay()
        )
    }

    @Test fun compareTo() {
        assertEquals(0, ExtendedClockTime(25,40).compareTo(ExtendedClockTime(25,40)))
        assertTrue(ExtendedClockTime(25,39) < ExtendedClockTime(25,40))
        assertTrue(ExtendedClockTime(24,41) < ExtendedClockTime(25,40))
        assertTrue(ExtendedClockTime(25,40) > ExtendedClockTime(25,39))
        assertTrue(ExtendedClockTime(25,40) > ExtendedClockTime(24,41))
    }

    @Test fun to_string() {
        assertEquals("00:00", ExtendedClockTime(0, 0).toString())
        assertEquals("40:10", ExtendedClockTime(40, 10).toString())
    }
}
