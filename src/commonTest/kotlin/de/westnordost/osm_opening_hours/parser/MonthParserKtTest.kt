package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.Month.December
import de.westnordost.osm_opening_hours.model.Month.February
import kotlin.test.Test
import kotlin.test.assertEquals

class MonthParserKtTest {

    @Test fun parseMonth() {
        assertEquals(February, parseMonth("Feb"))
        assertEquals(null, parseMonth("feb"))
        assertEquals(null, parseMonth("December"))
    }

    @Test fun parseMonth_lenient() {
        assertEquals(December, parseMonth("DEC", true))
        assertEquals(December, parseMonth("Dec.", true))
        assertEquals(December, parseMonth("dez", true))
        assertEquals(December, parseMonth("December", true))
        assertEquals(December, parseMonth("гру", true))
    }

    @Test fun does_not_consume_too_much() {
        verifyConsumption("Feb", false, StringWithCursor::parseMonth)
        verifyConsumption("гру", true, StringWithCursor::parseMonth)
        verifyConsumption("Dec.", true, StringWithCursor::parseMonth)
    }
}

private fun parseMonth(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseMonth(lenient)
