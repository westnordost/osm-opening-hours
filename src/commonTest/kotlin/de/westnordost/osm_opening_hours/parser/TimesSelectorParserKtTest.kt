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
        assertEquals(
            TimeSpan(VariableTime(Sunrise), VariableTime(Sunset)),
            parseTimesSelector("SUNUP - SUNDOWN", true)
        )
        assertEquals(Dusk - ClockTime(1, 0), parseTimesSelector("(DUSK-01:00)", true))
        assertEquals(StartingAtTime(ClockTime(12, 30)), parseTimesSelector("12h30+", true))
        assertEquals(ClockTime(10, 0)..VariableTime(Dusk), parseTimesSelector("10 to dusk", true))
        assertEquals(ClockTime(10, 0)..VariableTime(Dusk), parseTimesSelector("10 TO dusk", true))
        assertEquals(ClockTime(10, 0)..VariableTime(Dusk), parseTimesSelector("10—dusk", true))
        assertEquals(ClockTime(10, 30)..VariableTime(Dusk), parseTimesSelector("10：30〜dusk", true))

        assertEquals(
            TimeIntervals(VariableTime(Dusk), VariableTime(Dawn), IntervalMinutes(10)),
            parseTimesSelector("dusk-dawn / 10", true)
        )
        assertEquals(
            TimeIntervals(VariableTime(Dusk), VariableTime(Dawn), ClockTime(0, 10)),
            parseTimesSelector("dusk-dawn / 0:010", true)
        )
        assertEquals(
            TimeIntervals(VariableTime(Dusk), VariableTime(Dawn), ClockTime(2,0)),
            parseTimesSelector("dusk-dawn / 2h", true)
        )

        assertEquals(
            ClockTime(1, 0)..ExtendedClockTime(21, 0),
            parseTimesSelector("1am-9pm", true)
        )
    }

    @Test fun parseInterval() {
        assertEquals(IntervalMinutes(123), parseInterval("123"))
        assertEquals(ClockTime(12, 30), parseInterval("12:30"))
        assertEquals(ClockTime(1, 30), parseInterval("1:30", true))
        assertEquals(null, parseInterval("something else"))
    }

    @Test fun parseIntervalMinutes() {
        assertEquals(IntervalMinutes(123), parseIntervalMinutes("123"))
        assertEquals(null, parseIntervalMinutes("-123"))
        assertEquals(null, parseIntervalMinutes("something else"))
        assertFails { parseIntervalMinutes("0") }
    }

    @Test fun does_not_consume_too_much() {
        verifyConsumption("123", false, StringWithCursor::parseIntervalMinutes)
        verifyConsumption("123", false, StringWithCursor::parseInterval)
        verifyConsumption("00:30", false, StringWithCursor::parseInterval)

        verifyConsumption("00:30", false, StringWithCursor::parseTimesSelector)
        verifyConsumption("dusk", false, StringWithCursor::parseTimesSelector)
        verifyConsumption("(sunset+01:00)", false, StringWithCursor::parseTimesSelector)
        verifyConsumption("12:30+", false, StringWithCursor::parseTimesSelector)
        verifyConsumption("12:30-14:00", false, StringWithCursor::parseTimesSelector)
        verifyConsumption("12:30-14:00+", false, StringWithCursor::parseTimesSelector)
        verifyConsumption("12:30-14:00/10", false, StringWithCursor::parseTimesSelector)
    }
}

// convenience shortcuts
private fun parseIntervalMinutes(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseIntervalMinutes(lenient)

private fun parseInterval(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseInterval(lenient)

private fun parseTimesSelector(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseTimesSelector(lenient)
