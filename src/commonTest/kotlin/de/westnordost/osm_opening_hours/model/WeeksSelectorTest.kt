package de.westnordost.osm_opening_hours.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class WeekTest {

    @Test fun bounds() {
        assertFails { Week(0) }
        assertFails { Week(54) }
        Week(1)
        Week(53)
    }

    @Test fun to_string() {
        assertEquals("23", Week(23).toString())
        assertEquals("03", Week(3).toString())
    }
}

class WeekRangeTest {
    @Test fun bounds() {
        assertFails { WeekRange(2, 4, 0) }
        assertFails { WeekRange(2, 4, 54) }
        WeekRange(2, 4, 1)
        WeekRange(2, 4, 53)
    }

    @Test fun to_string() {
        assertEquals("10-15", (Week(10)..Week(15)).toString())
        assertEquals("10-15", WeekRange(10, 15, null).toString())
        assertEquals("10-15/2", WeekRange(10, 15, 2).toString())
        assertEquals("01-05", (Week(1)..Week(5)).toString())
    }
}
