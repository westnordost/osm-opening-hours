package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.*
import de.westnordost.osm_opening_hours.model.EventTime.*
import de.westnordost.osm_opening_hours.parser.Clock12.*

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class TimesSelectorParserKtTest {

    @Test fun parseTimesSelector() {
        assertEquals(ClockTime(12, 30), parseTimesSelector("12:30"))
        assertEquals(VariableTime(Dusk), parseTimesSelector("dusk"))
        assertEquals(Dusk - ClockTime(1, 0), parseTimesSelector("(dusk-01:00)"))

        assertEquals(StartingAtTime(ClockTime(12, 30)), parseTimesSelector("12:30+"))
        assertEquals(StartingAtTime(VariableTime(Sunset)), parseTimesSelector("sunset+"))
        assertEquals(StartingAtTime(Sunset + ClockTime(0, 1)), parseTimesSelector("(sunset+00:01)+"))

        assertEquals(
            TimeSpan(ClockTime(12, 30), VariableTime(Sunset)),
            parseTimesSelector("12:30-sunset")
        )
        assertEquals(
            TimeSpan(VariableTime(Dusk), VariableTime(Dawn)),
            parseTimesSelector("dusk - dawn")
        )
        assertEquals(
            TimeSpan(VariableTime(Dusk), VariableTime(Dawn), true),
            parseTimesSelector("dusk-dawn +")
        )

        assertEquals(
            TimeIntervals(VariableTime(Dusk), VariableTime(Dawn), IntervalMinutes(10)),
            parseTimesSelector("dusk-dawn / 10")
        )
        assertEquals(
            TimeIntervals(VariableTime(Dusk), VariableTime(Dawn), ClockTime(0, 10)),
            parseTimesSelector("dusk-dawn / 00:10")
        )

        assertEquals(null, parseTimesSelector("something else"))
        assertEquals(ClockTime(10, 0), parseTimesSelector("10:00—12:00"))
        assertEquals(ClockTime(10, 0), parseTimesSelector("10:00 to 12:00"))

        val cursor = StringWithCursor("dusk-dawn / 00:10+")
        cursor.parseTimesSelector(false)
        assertTrue(cursor.nextIs('+'))
    }

    @Test fun parseTimesSelector_lenient() {
        assertEquals(ClockTime(23, 30), parseTimesSelector("11:30 PM", true))
        assertEquals(VariableTime(Dusk), parseTimesSelector("DUSK", true))
        assertEquals(Dusk - ClockTime(1, 0), parseTimesSelector("(DUSK-01:00)", true))
        assertEquals(StartingAtTime(ClockTime(12, 30)), parseTimesSelector("12h30+", true))
        assertEquals(ClockTime(10, 0)..VariableTime(Dusk), parseTimesSelector("10 to dusk", true))
        assertEquals(ClockTime(10, 0)..VariableTime(Dusk), parseTimesSelector("10 TO dusk", true))
        assertEquals(ClockTime(10, 0)..VariableTime(Dusk), parseTimesSelector("10—dusk", true))
    }

    @Test fun parseInterval() {
        assertEquals(IntervalMinutes(123), parseInterval("123"))
        assertEquals(ClockTime(12, 30), parseInterval("12:30"))
        assertEquals(null, parseInterval("something else"))
    }

    @Test fun parseIntervalMinutes() {
        assertEquals(IntervalMinutes(123), parseIntervalMinutes("123"))
        assertEquals(null, parseIntervalMinutes("-123"))
        assertEquals(null, parseIntervalMinutes("something else"))
        assertFails { parseIntervalMinutes("0") }
    }

    @Test fun parseExtendedTime() {
        assertEquals(Dusk - ClockTime(1, 0), parseExtendedTime("(dusk-01:00)"))
        assertEquals(VariableTime(Dawn), parseExtendedTime("dawn"))
        assertEquals(ExtendedClockTime(31, 30), parseExtendedTime("31:30"))
        assertEquals(null, parseExtendedTime("something else"))
    }

    @Test fun parseTime() {
        assertEquals(Dusk - ClockTime(1, 0), parseTime("(dusk-01:00)"))
        assertEquals(VariableTime(Dawn), parseTime("dawn"))
        assertEquals(ClockTime(1, 30), parseTime("01:30"))
        assertEquals(null, parseTime("something else"))
    }

    @Test fun parseTime_lenient() {
        assertEquals(Dusk - ClockTime(1, 0), parseTime("(DUSK-01:00)", true))
        assertEquals(VariableTime(Dawn), parseTime("DAWN", true))
        assertEquals(ClockTime(13, 30), parseTime("01:30 pm", true))
    }

    @Test fun parseEventTimeWithOffset() {
        // whitespaces
        val sunsetPlus130 = Dusk + ClockTime(1, 30)
        assertEquals(sunsetPlus130, parseEventTimeWithOffset("(dusk+01:30)"))
        assertEquals(sunsetPlus130, parseEventTimeWithOffset("( dusk + 01:30 )"))
        assertEquals(sunsetPlus130, parseEventTimeWithOffset("(  dusk  +  01:30  )"))

        assertEquals(Dusk - ClockTime(1, 0), parseEventTimeWithOffset("(dusk-01:00)"))

        // missing braces
        assertEquals(null, parseEventTimeWithOffset("dusk+01:30"))

        assertFails { parseEventTimeWithOffset("(dusk+01:30") }
        assertFails { parseEventTimeWithOffset("(00:30)") }
        assertFails { parseEventTimeWithOffset("(dawn*00:30)") }
        assertFails { parseEventTimeWithOffset("(sunrise)") }
    }

    @Test fun parseEventTimeWithOffset_lenient() {
        assertEquals(Dusk - ClockTime(1, 0), parseEventTimeWithOffset("(DUSK-01:00)", true))
        assertFails { parseEventTimeWithOffset("(DUSK-01:00am)", true) }
    }

    @Test fun parseExtendedClockTime() {
        assertEquals(ExtendedClockTime(32, 45), parseExtendedClockTime("32:45"))
        assertFails { parseExtendedClockTime("49:45") }
        assertEquals(null, parseExtendedClockTime("something else"))
    }

    @Test fun parseClockTime() {
        assertEquals(ClockTime(0, 0), parseClockTime("00:00"))
        assertEquals(ClockTime(12, 45), parseClockTime("12:45"))
        assertEquals(ClockTime(12, 45), parseClockTime("12 : 45"))
        assertFails { parseClockTime("32:45") }
        assertEquals(null, parseClockTime("something else"))

        assertEquals(null, parseClockTime("8:30"))
        assertEquals(null, parseClockTime("8h30"))
        assertEquals(null, parseClockTime("12"))
        assertEquals(null, parseClockTime("12h"))
        assertEquals(null, parseClockTime("12:1"))
        assertEquals(null, parseClockTime("08:"))
        assertEquals(null, parseClockTime("08"))
        assertEquals(null, parseClockTime("08h"))
        assertEquals(null, parseClockTime("08."))
    }

    @Test fun parseClockTime_lenient() {
        assertEquals(ClockTime(8, 30), parseClockTime("8:30", true))
        assertEquals(ClockTime(8, 30), parseClockTime("8:30", true))
        assertEquals(ClockTime(8, 30), parseClockTime("8h30", true))
        assertEquals(ClockTime(8, 30), parseClockTime("8H30", true))
        assertEquals(ClockTime(8, 30), parseClockTime("8.30", true))
        assertEquals(ClockTime(8, 30), parseClockTime("8 . 30", true))
        assertEquals(ClockTime(8, 30), parseClockTime("8 h 30", true))
        assertEquals(ClockTime(12, 0), parseClockTime("12", true))
        assertEquals(ClockTime(12, 0), parseClockTime("12h", true))
        assertEquals(ClockTime(8, 0), parseClockTime("8 h", true))
        assertEquals(ClockTime(8, 0), parseClockTime("8 h", true))
        assertEquals(ClockTime(12, 0), parseClockTime("12H", true))
        assertEquals(ClockTime(8, 0), parseClockTime("8h", true))

        assertEquals(ClockTime(8, 0), parseClockTime("8 am", true))
        assertEquals(ClockTime(8, 0), parseClockTime("8 a.m.", true))
        assertEquals(ClockTime(8, 0), parseClockTime("8 A.M.", true))
        assertEquals(ClockTime(8, 0), parseClockTime("8 A.M", true))
        assertEquals(ClockTime(8, 0), parseClockTime("8AM", true))
        assertEquals(ClockTime(20, 0), parseClockTime("8 pm", true))
        assertEquals(ClockTime(8, 0), parseClockTime("8h am", true))

        assertEquals(ClockTime(12, 0), parseClockTime("12 pm", true))
        assertEquals(ClockTime(0, 0), parseClockTime("12 am", true))
        assertEquals(ClockTime(12, 30), parseClockTime("12:30 pm", true))
        assertEquals(ClockTime(12, 30), parseClockTime("12h30 pm", true))
        assertEquals(ClockTime(0, 30), parseClockTime("12.30 am", true))

        assertEquals(ClockTime(14, 0), parseClockTime("14 pm", true))

        assertEquals(ClockTime(4, 0), parseClockTime("4 ㏂", true))
        assertEquals(ClockTime(16, 0), parseClockTime("4 ㏘", true))

        assertEquals(ClockTime(8, 30), parseClockTime("8:30 am", true))
        assertEquals(ClockTime(8, 30), parseClockTime("8h30 am", true))
        assertEquals(ClockTime(8, 30), parseClockTime("8.30 am", true))

        assertEquals(ClockTime(8), parseClockTime("8 30", true))

        assertEquals(null, parseClockTime("08:", true))
        assertEquals(null, parseClockTime("12:1", true))
    }

    @Test fun parseEventTime() {
        assertEquals(null, parseEventTime("sunblock"))
        assertEquals(null, parseEventTime("sunsetting"))
        assertEquals(null, parseEventTime("Sunset"))
        assertEquals(Sunset, parseEventTime("sunset"))
    }
    
    @Test fun parseEventTime_lenient() {
        assertEquals(Sunset, parseEventTime("Sunset", true))
        assertEquals(Sunset, parseEventTime("sundown", true))
        assertEquals(Dusk, parseEventTime("DUSK", true))
    }

    @Test fun parseAmPm() {
        assertEquals(null, parseAmPm("something"))
        assertEquals(null, parseAmPm("a"))
        assertEquals(null, parseAmPm("p"))

        listOf("㏂", "AM", "am", "a.m", "a.m.", "a. m.", "A.M", "A. M.").forEach {
            assertEquals(AM, parseAmPm(it), it)
        }

        listOf("㏘", "PM", "pm", "p.m", "p.m.", "p. m.", "P.M", "P. M.",).forEach {
            assertEquals(PM, parseAmPm(it), it)
        }
    }

    @Test fun does_not_consume_too_much() {
        verifyConsumption("sunset", false, StringWithCursor::parseEventTime)
        verifyConsumption("(sunset+01:00)", false, StringWithCursor::parseVariableTime)

        verifyConsumption("00:30", false, StringWithCursor::parseClockTime)
        verifyConsumption("5", true, StringWithCursor::parseClockTime)
        verifyConsumption("40:30", false, StringWithCursor::parseExtendedClockTime)
        verifyConsumption("40", true, StringWithCursor::parseExtendedClockTime)

        verifyConsumption("00:30", false, StringWithCursor::parseTime)
        verifyConsumption("5", true, StringWithCursor::parseTime)
        verifyConsumption("40:30", false, StringWithCursor::parseExtendedTime)
        verifyConsumption("40", true, StringWithCursor::parseExtendedTime)

        verifyConsumption("123", StringWithCursor::parseIntervalMinutes)
        verifyConsumption("123", StringWithCursor::parseInterval)
        verifyConsumption("00:30", StringWithCursor::parseInterval)

        verifyConsumption("00:30", false, StringWithCursor::parseTimesSelector)
        verifyConsumption("dusk", false, StringWithCursor::parseTimesSelector)
        verifyConsumption("(sunset+01:00)", false, StringWithCursor::parseTimesSelector)
        verifyConsumption("12:30+", false, StringWithCursor::parseTimesSelector)
        verifyConsumption("12:30-14:00", false, StringWithCursor::parseTimesSelector)
        verifyConsumption("12:30-14:00+", false, StringWithCursor::parseTimesSelector)
        verifyConsumption("12:30-14:00/10", false, StringWithCursor::parseTimesSelector)

        verifyConsumption("a.", expectCursorPosAt = 0, StringWithCursor::parseAmPm)
        verifyConsumption("am", StringWithCursor::parseAmPm)
        verifyConsumption("p.", expectCursorPosAt = 0, StringWithCursor::parseAmPm)
        verifyConsumption("p.m.", StringWithCursor::parseAmPm)
    }
}

// convenience shortcuts
private fun parseIntervalMinutes(s: String) = StringWithCursor(s).parseIntervalMinutes()
private fun parseInterval(s: String) = StringWithCursor(s).parseInterval()

private fun parseEventTime(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseEventTime(lenient)

private fun parseClockTime(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseClockTime(lenient)

private fun parseExtendedClockTime(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseExtendedClockTime(lenient)

private fun parseEventTimeWithOffset(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseVariableTime(lenient)

private fun parseExtendedTime(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseExtendedTime(lenient)

private fun parseTime(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseTime(lenient)

private fun parseTimesSelector(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseTimesSelector(lenient)

private fun parseAmPm(s: String) =
    StringWithCursor(s).parseAmPm()