package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.*
import de.westnordost.osm_opening_hours.model.Weekday.*
import de.westnordost.osm_opening_hours.model.Holiday.*
import de.westnordost.osm_opening_hours.model.Month.*
import de.westnordost.osm_opening_hours.model.AnnualEvent.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class SelectorParserKtTest {

    @Test fun parseSelector_247() {
        assertEquals(TwentyFourSeven, parseSelector("24/7"))
        assertFails { parseSelector("24/7 ,") }
    }

    @Test fun parseSelector_247_lenient() {
        assertEquals(TwentyFourSeven, parseSelector("24/7,", true))
    }

    @Test fun parseSelector_Range_text() {
        assertEquals(Range(text = "A"), parseSelector("\"A\":"))
        assertEquals(Range(text = "A"), parseSelector("\"A\"  :"))
        assertEquals(true, parseSelector("\"A\"").isEmpty())

        assertEquals(true, parseSelector("\"A\" Mo").isEmpty())
        assertEquals(Range(text = "A", weekdays = listOf(Monday)), parseSelector("\"A\": Mo"))

        assertFails { parseSelector("\"A\":,") }
        assertFails { parseSelector("\"A\": Mo,") }
    }

    @Test fun parseSelector_Range_text_lenient() {
        assertEquals(
            Range(text = "A", weekdays = listOf(Monday), times = listOf(ClockTime(10, 0))),
            parseSelector("\"A\": Mo:10:00", true)
        )
        assertEquals(
            Range(text = "A", weekdays = listOf(Monday), times = listOf(ClockTime(10, 0))),
            parseSelector("\"A\"： Mo:10:00", true)
        )
        assertEquals(Range(text = "A"), parseSelector("\"A\":,", true))
    }

    @Test fun parseSelector_Range_weekdays_and_times() {
        assertEquals(Range(weekdays = listOf(Monday)), parseSelector("Mo"))

        assertEquals(Range(times = listOf(ClockTime(8, 0))), parseSelector("08:00"))
        assertEquals(
            Range(times = listOf(ClockTime(8, 0), ClockTime(9, 0))),
            parseSelector("08:00,09:00")
        )

        assertEquals(
            Range(weekdays = listOf(Monday), times = listOf(ClockTime(8, 0))),
            parseSelector("Mo08:00")
        )
        assertEquals(
            Range(weekdays = listOf(Monday), times = listOf(ClockTime(8, 0))),
            parseSelector("Mo08:00")
        )
        assertEquals(
            Range(weekdays = listOf(Monday)),
            parseSelector("Mo:08:00")
        )
        assertEquals(
            Range(weekdays = listOf(Monday)),
            parseSelector("Mo 24/7")
        )

        assertEquals(Range(), parseSelector("Mosunset"))

        assertFails { parseSelector("Mo,") }
    }

    @Test fun parseSelector_Range_weekdays_and_times_lenient() {
        assertEquals(
            Range(weekdays = listOf(Monday), times = listOf(ClockTime(8, 0))),
            parseSelector("Mo:08:00", true)
        )
        assertEquals(
            Range(weekdays = listOf(Monday), times = listOf(ClockTime(8, 0))),
            parseSelector("Mo：08：00", true)
        )
        assertEquals(
            Range(weekdays = listOf(Monday), times = listOf(ClockTime(0)..ExtendedClockTime(24))),
            parseSelector("Mo24/7", true)
        )
        assertEquals(Range(), parseSelector("Mosunset", true))

        assertEquals(Range(weekdays = listOf(Monday)), parseSelector("Mo,", true))
    }

    @Test fun parseSelector_Range_years_months_weeks() {
        // year
        assertEquals(Range(years = listOf(Year(1999))), parseSelector("1999"))
        assertEquals(Range(years = listOf(Year(1999)), useSeparatorForReadability = true), parseSelector("1999:"))
        assertEquals(Range(years = listOf(Year(1999), Year(2005))), parseSelector("1999,2005"))

        // months
        assertEquals(Range(months = listOf(SingleMonth(June))), parseSelector("Jun"))
        assertEquals(Range(months = listOf(SingleMonth(June)), useSeparatorForReadability = true), parseSelector("Jun:"))
        assertEquals(Range(months = listOf(SingleMonth(June), SingleMonth(July))), parseSelector("Jun,Jul"))

        // weeks
        assertEquals(Range(weeks = listOf(Week(12))), parseSelector("week12"))
        assertEquals(Range(weeks = listOf(Week(12)), useSeparatorForReadability = true), parseSelector("week12:"))
        assertEquals(Range(weeks = listOf(Week(12), Week(13))), parseSelector("week12,13"))

        // years + months
        assertEquals(Range(months = listOf(SingleMonth(2000, June))), parseSelector("2000 Jun"))
        assertEquals(
            Range(years = listOf(YearRange(2000, 2022)), months = listOf(SingleMonth(June))),
            parseSelector("2000-2022 Jun")
        )
        assertEquals(
            Range(years = listOf(Year(2000), Year(2022)), months = listOf(SingleMonth(June))),
            parseSelector("2000, 2022 Jun")
        )
        assertEquals(
            Range(years = listOf(Year(2000)), months = listOf(SingleMonth(2022, June))),
            parseSelector("2000 2022 Jun")
        )
        assertEquals(
            Range(
                years = listOf(Year(2000)),
                months = listOf(CalendarDate(2001, June, 5)..CalendarDate(2020, July, 6))
            ),
            parseSelector("2000 2001 Jun 05 - 2020 Jul 06")
        )
        assertEquals(
            Range(months = listOf(CalendarDate(2000, June, 5)..CalendarDate(2020,July, 6))),
            parseSelector("2000 Jun 05 - 2020 Jul 06")
        )
        assertEquals(
            Range(
                years = listOf(Year(2000)),
                months = listOf(SingleMonth(June), CalendarDate(July, 5)..CalendarDate(August, 8))
            ),
            parseSelector("2000 Jun, Jul 05 - Aug 08")
        )
        assertEquals(
            Range(months = listOf(SingleMonth(2000, June), CalendarDate(July, 5), VariableDate(2003, Easter))),
            parseSelector("2000 Jun, Jul 05, 2003 easter")
        )

        // years + weeks
        assertEquals(
            Range(years = listOf(Year(1999)), weeks = listOf(Week(12))),
            parseSelector("1999 week12")
        )
        assertEquals(
            Range(years = listOf(Year(1999)), weeks = listOf(Week(12)), useSeparatorForReadability = true),
            parseSelector("1999 week12:")
        )
        assertEquals(
            Range(years = listOf(Year(1999), Year(2000)), weeks = listOf(Week(12), Week(13))),
            parseSelector("1999,2000 week12,13")
        )
        assertEquals(
            Range(years = listOf(Year(1999), Year(2000)), weeks = listOf(Week(12), Week(13))),
            parseSelector("1999,2000week12,13")
        )

        // years + months + weeks
        assertEquals(
            Range(
                years = listOf(Year(1999), Year(2000)),
                months = listOf(SingleMonth(July), VariableDate(Easter), CalendarDate(August, 5)),
                weeks = listOf(Week(12), Week(13))),
            parseSelector("1999,2000Jul,easter,Aug05week12,13")
        )
        assertEquals(
            Range(
                years = listOf(Year(1999), Year(2000)),
                months = listOf(SingleMonth(July), VariableDate(Easter), CalendarDate(August, 5)),
                weeks = listOf(Week(12), Week(13))),
            parseSelector("1999 , 2000 Jul , easter , Aug 05 week 12 , 13 ")
        )

        // not allowed in the spec
        assertFails {
            parseSelector("2000 Jul - 2001 Aug")
        }
    }

    @Test fun parseSelector_Range_years_months_weeks_lenient() {
        assertEquals(
            Range(years = listOf(Year(1999)), useSeparatorForReadability = true),
            parseSelector("1999:", true)
        )
        assertEquals(
            Range(weeks = listOf(Week(12)), useSeparatorForReadability = true),
            parseSelector("week12:", true)
        )
        assertEquals(
            Range(years = listOf(Year(1999)), weeks = listOf(Week(12)), useSeparatorForReadability = true),
            parseSelector("1999 week12 :", true)
        )
        assertEquals(
            Range(weeks = listOf(Week(11), WeekRange(12, 14))),
            parseSelector("week 11, week 12-14", true)
        )
    }

    @Test fun parseSelector_Range_wideRange_weekdays() {
        assertEquals(
            Range(years = listOf(Year(1999)), weekdays = listOf(Monday), useSeparatorForReadability = false),
            parseSelector("1999 Mo")
        )
        assertEquals(
            Range(years = listOf(Year(1999)), weekdays = listOf(Monday), useSeparatorForReadability = true),
            parseSelector("1999 : Mo")
        )
        assertEquals(
            Range(years = listOf(Year(1999)), weekdays = listOf(Monday), useSeparatorForReadability = false),
            parseSelector("1999Mo")
        )
    }

    @Test fun parseComment() {
        assertEquals("hallo", parseComment("\"hallo\""))
        assertEquals("hallo", parseComment("\"hallo\"something more"))
        assertEquals(null, parseComment("hallo"))
        assertFails { parseComment("\"hallo")  }
    }

    @Test fun parseWeekdaysAndHolidaysSelector() {
        assertEquals(WeekdaysAndHolidays(listOf(Monday)),
            parseWeekdaysAndHolidaysSelector("Mo")
        )
        assertEquals(
            WeekdaysAndHolidays(holidays = listOf(PublicHoliday)),
            parseWeekdaysAndHolidaysSelector("PH")
        )

        assertEquals(WeekdaysAndHolidays(listOf(Monday, Tuesday)),
            parseWeekdaysAndHolidaysSelector("Mo,Tu")
        )
        assertEquals(WeekdaysAndHolidays(listOf(Monday, Tuesday)),
            parseWeekdaysAndHolidaysSelector("Mo , Tu")
        )
        assertEquals(WeekdaysAndHolidays(listOf(Monday, Tuesday)),
            parseWeekdaysAndHolidaysSelector("Mo , Tu ,")
        )

        assertEquals(WeekdaysAndHolidays(listOf(Monday), listOf(PublicHoliday)),
            parseWeekdaysAndHolidaysSelector("PH,Mo")
        )
        assertEquals(WeekdaysAndHolidays(listOf(Monday), listOf(PublicHoliday)),
            parseWeekdaysAndHolidaysSelector("PH , Mo")
        )
        assertEquals(WeekdaysAndHolidays(listOf(Monday), listOf(PublicHoliday)),
            parseWeekdaysAndHolidaysSelector("PH , Mo , ")
        )

        assertEquals(
            WeekdaysAndHolidays(listOf(Monday), listOf(PublicHoliday)),
            parseWeekdaysAndHolidaysSelector("Mo,PH")
        )
        assertEquals(
            WeekdaysAndHolidays(listOf(Monday), listOf(PublicHoliday)),
            parseWeekdaysAndHolidaysSelector("Mo , PH")
        )
        assertEquals(
            WeekdaysAndHolidays(listOf(Monday), listOf(PublicHoliday)),
            parseWeekdaysAndHolidaysSelector("Mo , PH , ")
        )

        assertEquals(WeekdaysAndHolidays(holidays = listOf(PublicHoliday)),
            parseWeekdaysAndHolidaysSelector("PH,something")
        )
        assertEquals(WeekdaysAndHolidays(holidays = listOf(PublicHoliday)),
            parseWeekdaysAndHolidaysSelector("PH , something")
        )
        assertEquals(WeekdaysAndHolidays(listOf(Monday)),
            parseWeekdaysAndHolidaysSelector("Mo,something")
        )
        assertEquals(WeekdaysAndHolidays(listOf(Monday)),
            parseWeekdaysAndHolidaysSelector("Mo , something")
        )

        assertEquals(
            WeekdaysAndHolidays(listOf(Monday), listOf(PublicHoliday), true),
            parseWeekdaysAndHolidaysSelector("PH Mo")
        )
        assertEquals(
            WeekdaysAndHolidays(listOf(Monday), listOf(PublicHoliday), true),
            parseWeekdaysAndHolidaysSelector("PH   Mo")
        )
        assertEquals(
            WeekdaysAndHolidays(listOf(Monday, Tuesday), listOf(PublicHoliday, SchoolHoliday), true),
            parseWeekdaysAndHolidaysSelector("PH,SH Mo,Tu")
        )
        assertEquals(
            WeekdaysAndHolidays(listOf(Monday, Tuesday), listOf(PublicHoliday, SchoolHoliday), true),
            parseWeekdaysAndHolidaysSelector("PH  ,  SH    Mo , Tu")
        )

        assertFails { parseWeekdaysAndHolidaysSelector("Mo,PH,Tu") }
        assertFails { parseWeekdaysAndHolidaysSelector("PH,Tu,SH") }
    }

    @Test fun parseWeekdaysAndHolidaysSelector_lenient() {
        assertEquals(WeekdaysAndHolidays(listOf(Monday)),
            parseWeekdaysAndHolidaysSelector("mo", true)
        )
        assertEquals(WeekdaysAndHolidays(holidays = listOf(PublicHoliday)),
            parseWeekdaysAndHolidaysSelector("ph", true)
        )
        assertEquals(WeekdaysAndHolidays(listOf(Monday, Tuesday), listOf(PublicHoliday)),
            parseWeekdaysAndHolidaysSelector("mo,ph,tu", true)
        )
    }

    @Test fun does_not_consume_too_much() {
        verifyConsumption("\"yo\"", StringWithCursor::parseComment)
        verifyConsumption("open", false, StringWithCursor::parseRuleType)

        verifyConsumption("something", false, expectCursorPosAt = 0, StringWithCursor::parseWeekdaysAndHolidaysSelector)
        verifyConsumption("PH", false, StringWithCursor::parseWeekdaysAndHolidaysSelector)
        verifyConsumption("PH, SH", false, StringWithCursor::parseWeekdaysAndHolidaysSelector)
        verifyConsumption("PH, SH, Mo", false, StringWithCursor::parseWeekdaysAndHolidaysSelector)
        verifyConsumption("Mo", false, StringWithCursor::parseWeekdaysAndHolidaysSelector)
        verifyConsumption("PH Mo", false, StringWithCursor::parseWeekdaysAndHolidaysSelector)

        verifyConsumption("PH something", false, expectCursorPosAt = 2, StringWithCursor::parseWeekdaysAndHolidaysSelector)
        verifyConsumption("PH Mo,something", false, expectCursorPosAt = 5, StringWithCursor::parseWeekdaysAndHolidaysSelector)

        verifyConsumption("24/7", false, StringWithCursor::parseSelector)
        verifyConsumption("\"yo\"", false, expectCursorPosAt = 0, StringWithCursor::parseSelector)
        verifyConsumption("\"yo\":", false, expectCursorPosAt = 5, StringWithCursor::parseSelector)
        verifyConsumption("2000", false, StringWithCursor::parseSelector)
        verifyConsumption("2000, 2001-2005", false, StringWithCursor::parseSelector)
        verifyConsumption(
            "2000, 2001-2005 open, something",
            false, expectCursorPosAt = 15, StringWithCursor::parseSelector
        )
        verifyConsumption("Jan", false, StringWithCursor::parseSelector)
        verifyConsumption("week 34", false, StringWithCursor::parseSelector)
        verifyConsumption("week 34,45", false, StringWithCursor::parseSelector)
        verifyConsumption("week 34:", false, StringWithCursor::parseSelector)
        verifyConsumption("week 34:", false, StringWithCursor::parseSelector)
        verifyConsumption(":", false, StringWithCursor::parseSelector)
        verifyConsumption("Mo", false, StringWithCursor::parseSelector)
        verifyConsumption("Mo:", true, StringWithCursor::parseSelector)
        verifyConsumption("12:00", false, StringWithCursor::parseSelector)
        verifyConsumption("Mo: 24/7", true, StringWithCursor::parseSelector)
    }
}

// convenience shortcuts
private fun parseComment(s: String) = StringWithCursor(s).parseComment()

private fun parseSelector(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseSelector(lenient)

private fun parseWeekdaysAndHolidaysSelector(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseWeekdaysAndHolidaysSelector(lenient)

