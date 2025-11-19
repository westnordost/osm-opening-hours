package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.AnnualEvent.Easter
import de.westnordost.osm_opening_hours.model.CalendarDate
import de.westnordost.osm_opening_hours.model.LastNth
import de.westnordost.osm_opening_hours.model.Month.June
import de.westnordost.osm_opening_hours.model.NextWeekday
import de.westnordost.osm_opening_hours.model.Nth
import de.westnordost.osm_opening_hours.model.PreviousWeekday
import de.westnordost.osm_opening_hours.model.SpecificWeekdayDate
import de.westnordost.osm_opening_hours.model.VariableDate
import de.westnordost.osm_opening_hours.model.Weekday.Friday
import de.westnordost.osm_opening_hours.model.Weekday.Sunday
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class DateParserKtTest {

    @Test fun parseDate() {
        assertEquals(CalendarDate(June, 1), parseDate("Jun01"))
        assertEquals(CalendarDate(June, 1), parseDate("Jun 01"))
        assertEquals(VariableDate(Easter), parseDate("easter"))
        assertEquals(VariableDate(Easter, dayOffset = 1), parseDate("easter +1day"))
        assertEquals(
            VariableDate(Easter, weekdayOffset = NextWeekday(Friday)),
            parseDate("easter +Fr")
        )
        assertEquals(
            VariableDate(Easter, weekdayOffset = NextWeekday(Friday), dayOffset = -3),
            parseDate("easter +Fr -3 days")
        )
        assertEquals(null, parseDate("EASTER"))

        assertEquals(
            CalendarDate(June, 1, NextWeekday(Sunday)),
            parseDate("Jun 01+Su")
        )
        assertEquals(
            CalendarDate(June, 1, NextWeekday(Sunday)),
            parseDate("Jun 01 +Su")
        )

        assertEquals(
            CalendarDate(June, 1, PreviousWeekday(Sunday)),
            parseDate("Jun 01-Su")
        )
        assertEquals(
            CalendarDate(June, 1, PreviousWeekday(Sunday)),
            parseDate("Jun 01 -Su")
        )

        assertEquals(
            CalendarDate(June, 1, dayOffset = 1),
            parseDate("Jun 01 +1 day")
        )
        assertEquals(
            CalendarDate(June, 1, NextWeekday(Sunday), dayOffset = 1),
            parseDate("Jun 01 +Su +1 day")
        )
        assertEquals(
            CalendarDate(June, 1, NextWeekday(Sunday), dayOffset = 1),
            parseDate("Jun 01 +Su+1 day")
        )

        assertEquals(SpecificWeekdayDate(June, Friday, Nth(1)), parseDate("Jun Fr[1]"))
        assertEquals(SpecificWeekdayDate(June, Friday, LastNth(1)), parseDate("Jun Fr[-1]"))
        assertEquals(SpecificWeekdayDate(2000, June, Friday, Nth(1)), parseDate("Jun Fr[1]", false, 2000))
        assertEquals(SpecificWeekdayDate(June, Friday, LastNth(1)), parseDate("Jun Fr [ - 1 ] "))
        assertEquals(SpecificWeekdayDate(June, Friday, Nth(1), -3), parseDate("Jun Fr[1] -3 days"))
        assertEquals(null, parseDate("Jun Fr[1 - 2]"))
        assertEquals(null, parseDate("Jun Fr[1 , 2]"))
        assertEquals(null, parseDate("Jun Fr[1] , PH"))
        assertEquals(null, parseDate("Jun Fr[1] , Fr"))

        assertEquals(null, parseDate("Jun01:00"))
        assertEquals(null, parseDate("Jun24/7"))

        assertFails { parseDate("Jun Fr [") }
        assertFails { parseDate("Jun 1") }
        assertFails { parseDate("Jun 32") }
    }

    @Test fun parseDate_lenient() {
        assertEquals(CalendarDate(June, 1), parseDate("Jun 1", true))
        assertEquals(VariableDate(Easter), parseDate("EASTER", true))
        assertEquals(CalendarDate(June, 1), parseDate("Jun １", true))

        assertEquals(null, parseDate("Jun01:00", true))
        assertEquals(null, parseDate("Jun24/7"))
    }

    @Test fun parseAnnualEvent() {
        assertEquals(Easter, parseAnnualEvent("easter"))
        assertEquals(null, parseAnnualEvent("EASTER"))
        assertEquals(null, parseAnnualEvent("something"))
    }

    @Test fun parseAnnualEvent_lenient() {
        assertEquals(Easter, parseAnnualEvent("EASTER", true))
        assertEquals(Easter, parseAnnualEvent("Easter", true))
    }

    @Test fun does_not_consume_too_much() {
        verifyConsumption("easter", false, StringWithCursor::parseAnnualEvent)
        verifyConsumption("Jun 01") { parseDate(false, null) }
        verifyConsumption("Jun 01 -Su") { parseDate(false, null) }
    }
}

private fun parseDate(s: String, lenient: Boolean = false, year: Int? = null) =
    StringWithCursor(s).parseDate(lenient, year)

private fun parseAnnualEvent(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseAnnualEvent(lenient)
