package de.westnordost.osm_opening_hours.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

import de.westnordost.osm_opening_hours.model.Weekday.*
import de.westnordost.osm_opening_hours.model.Holiday.*

class HolidaySelectorTest {
    @Test fun to_string() {
        assertEquals("PH", PublicHoliday.toString())
        assertEquals("PH -1 day", HolidayWithOffset(PublicHoliday, -1).toString())
        assertEquals("SH +2 days", HolidayWithOffset(SchoolHoliday, 2).toString())
    }
}
