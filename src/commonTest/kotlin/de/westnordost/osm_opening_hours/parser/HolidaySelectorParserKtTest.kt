package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.Holiday.*
import de.westnordost.osm_opening_hours.model.HolidayWithOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class HolidaySelectorParserKtTest {
    @Test fun parseHolidaySelector() {
        assertEquals(PublicHoliday, parseHolidaySelector("PH"))
        assertEquals(HolidayWithOffset(PublicHoliday, +1), parseHolidaySelector("PH +1 day"))
        assertEquals(HolidayWithOffset(PublicHoliday, +1), parseHolidaySelector("PH+1day"))
        assertEquals(null, parseHolidaySelector("something"))
        assertEquals(null, parseHolidaySelector("ph"))
    }

    @Test fun parseHolidaySelector_lenient() {
        assertEquals(PublicHoliday, parseHolidaySelector("ph", true))
    }

    @Test fun does_not_consume_too_much() {
        verifyConsumption("PH", false, StringWithCursor::parseHolidaySelector)
        verifyConsumption("PH +3 days", false, StringWithCursor::parseHolidaySelector)
    }
}

private fun parseHolidaySelector(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseHolidaySelector(lenient)
