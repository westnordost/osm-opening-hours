package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.*
import de.westnordost.osm_opening_hours.model.Month.*
import de.westnordost.osm_opening_hours.model.Weekday.*
import de.westnordost.osm_opening_hours.model.AnnualEvent.*

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class MonthsOrDateSelectorParserKtTest {

    @Test fun parseMonthsOrDatesSelector() {
        assertEquals(null, parse("something else"))

        assertEquals(CalendarDate(June, 1), parse("Jun01"))
        assertEquals(CalendarDate(2000, June, 1), parse("2000Jun01"))

        assertEquals(StartingAtDate(CalendarDate(June, 1)), parse("Jun01+"))
        assertEquals(StartingAtDate(CalendarDate(June, 1)), parse("Jun01  +"))

        assertEquals(DatesInMonth(June, MonthDayRange(1, 5)), parse("Jun01-05"))
        assertEquals(DatesInMonth(June, MonthDayRange(1, 5)), parse("Jun01 - 05"))

        assertEquals(CalendarDate(April, 1), parse("Apr 01: Sa"))
        assertEquals(CalendarDate(April, 1), parse("Apr 01: Sa", true))

        assertEquals(SingleMonth(June), parse("Jun01:00"))
        assertEquals(SingleMonth(June), parse("Jun24/7"))

        assertEquals(
            CalendarDate(June, 1)..CalendarDate(July, 5),
            parse("Jun01-Jul05")
        )
        assertEquals(
            CalendarDate(June, 1)..CalendarDate(2020, July, 5),
            parse("Jun01-2020Jul05")
        )
        assertEquals(
            CalendarDate(1999, June, 1)..CalendarDate(2020, July, 5),
            parse("1999Jun01-2020Jul05")
        )

        assertEquals(SpecificWeekdayDate(June, Friday, Nth(1)), parse("Jun Fr[1]"))
        assertEquals(SpecificWeekdayDate(June, Friday, LastNth(1)), parse("Jun Fr[-1]"))
        assertEquals(SpecificWeekdayDate(2000, June, Friday, Nth(1)), parse("2000 Jun Fr[1]"))
        assertEquals(SingleMonth(June), parse("Jun Fr[1 - 2]"))
        assertEquals(SingleMonth(June), parse("Jun Fr[1 , 2]"))
        assertEquals(SingleMonth(June), parse("Jun Fr[1] , PH"))
        assertEquals(SingleMonth(June), parse("Jun Fr[1] , Fr"))

        assertEquals(SingleMonth(June), parse("Jun"))
        assertEquals(June..July, parse("Jun-Jul"))
        assertEquals(June..July, parse("Jun - Jul"))

        assertEquals(CalendarDate(June, 1), parse("Jun01—05"))
        assertEquals(SingleMonth(June), parse("Jun—Jul"))
        assertFails { parse("Jun01-") }
        assertFails { parse("Jun-") }
        assertFails { parse("Jun01-") }
        assertFails { parse("Jun01-5") }
        assertFails { parse("Jun01-1899Jul05") }
        assertFails { parse("Jun01-300Jul05") }
        assertFails { parse("Jun01-99999Jul05") }
    }

    @Test fun parseMonthsOrDatesSelector_lenient() {
        assertEquals(DatesInMonth(June, MonthDayRange(1, 5)), parse("Jun01-5", true))
        assertEquals(DatesInMonth(June, MonthDayRange(1, 5)), parse("Jun01—05", true))
        assertEquals(June..July, parse("Jun—Jul", true))
    }

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

        assertFails { parseDate("Jun Fr [") }
        assertFails { parseDate("Jun 1") }
        assertFails { parseDate("Jun 32") }
    }

    @Test fun parseDate_lenient() {
        assertEquals(CalendarDate(June, 1), parseDate("Jun 1", true))
        assertEquals(VariableDate(Easter), parseDate("EASTER", true))
    }

    @Test fun parseDatesInMonth() {
        assertEquals(null, parseDatesInMonth("something"))
        assertEquals(null, parseDatesInMonth("Jun"))
        assertEquals(null, parseDatesInMonth("Jun 01")) // normal date
        assertEquals(DatesInMonth(June, MonthDayRange(1,4)), parseDatesInMonth("Jun 01-04"))
        assertEquals(DatesInMonth(June, MonthDayRange(1,4)), parseDatesInMonth("Jun 01 - 04"))
        assertEquals(DatesInMonth(2000, June, MonthDayRange(1,4)), parseDatesInMonth("Jun 01-04", year = 2000))

        assertFails { parseDatesInMonth("Jun 01,02") }
        assertFails { parseDatesInMonth("Jun 01 , 02") }
        assertFails { parseDatesInMonth("Jun 01-02,04") }
    }

    @Test fun parseDatesInMonth_lenient() {
        assertEquals(
            DatesInMonth(June, MonthDay(1), MonthDay(2)),
            parseDatesInMonth("Jun 1,2", true)
        )
        assertEquals(
            DatesInMonth(June, MonthDay(1), MonthDay(2)),
            parseDatesInMonth("Jun 1 , 2", true)
        )
        assertEquals(
            DatesInMonth(June, MonthDayRange(1,2), MonthDay(4)),
            parseDatesInMonth("Jun 1-2,4", true)
        )
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

    @Test fun parseMonth() {
        assertEquals(February, parseMonth("Feb"))
        assertEquals(null, parseMonth("feb"))
        assertEquals(null, parseMonth("something"))
    }

    @Test fun parseMonth_lenient() {
        assertEquals(December, parseMonth("DEC", true))
        assertEquals(December, parseMonth("Dec.", true))
        assertEquals(December, parseMonth("dez", true))
        assertEquals(December, parseMonth("December", true))
    }

    @Test fun does_not_consume_too_much() {
        verifyConsumption("Feb", false, StringWithCursor::parseMonth)
        verifyConsumption("easter", false, StringWithCursor::parseAnnualEvent)
        verifyConsumption("Jun 01") { parseDate(false, null) }
        verifyConsumption("Jun 01 -Su") { parseDate(false, null) }

        verifyConsumption("Jun 05:00", false, expectCursorPosAt = 3, StringWithCursor::parseMonthsOrDatesSelector)
        verifyConsumption("Jun 24/7", false, expectCursorPosAt = 3, StringWithCursor::parseMonthsOrDatesSelector)

        verifyConsumption("Jun 01,02") { parseDatesInMonth(true, null) }
        verifyConsumption("Jun 01-02") { parseDatesInMonth(false, null) }

        listOf(
            "Jun 01",
            "Jun 01 +",
            "Jun 01 - 02",
            "Jun 01 - Jul 02",
            "Jun 01 - 2020 Jul 02",
            "Jun",
            "Jun - Jul",
        ).forEach {
            verifyConsumption(it, false, StringWithCursor::parseMonthsOrDatesSelector)
        }
    }
}

// convenience shortcuts
private fun parse(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseMonthsOrDatesSelector(lenient)

private fun parseDate(s: String, lenient: Boolean = false, year: Int? = null) =
    StringWithCursor(s).parseDate(lenient, year)

private fun parseMonth(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseMonth(lenient)

private fun parseDatesInMonth(s: String, lenient: Boolean = false, year: Int? = null) =
    StringWithCursor(s).parseDatesInMonth(lenient, year)

private fun parseAnnualEvent(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseAnnualEvent(lenient)