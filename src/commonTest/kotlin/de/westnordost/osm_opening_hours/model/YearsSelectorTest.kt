package de.westnordost.osm_opening_hours.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class YearTest {

    @Test fun bounds() {
        assertFails { Year(1900) }
        assertFails { Year(10000) }
        Year(1901)
        Year(9999)
    }

    @Test fun to_string() {
        assertEquals("2015", Year(2015).toString())
    }
}

class StartingAtYearTest {
    @Test fun bounds() {
        assertFails { StartingAtYear(1900) }
        assertFails { StartingAtYear(10000) }
        StartingAtYear(1901)
        StartingAtYear(9999)
    }

    @Test fun to_string() {
        assertEquals("2015+", StartingAtYear(2015).toString())
    }
}

class YearRangeTest {
    @Test fun bounds() {
        assertFails { YearRange(2000, 3000, 0) }
        assertFails { YearRange(2000, 1999) }
    }

    @Test fun to_string() {
        assertEquals("2015-2022", (Year(2015)..Year(2022)).toString())
        assertEquals("2015-2022", YearRange(2015, 2022, null).toString())
        assertEquals("2015-2022/2", YearRange(2015, 2022, 2).toString())
    }
}
