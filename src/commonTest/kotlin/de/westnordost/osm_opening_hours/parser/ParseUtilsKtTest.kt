package de.westnordost.osm_opening_hours.parser

import kotlin.test.Test
import kotlin.test.assertEquals

class ParseUtilsKtTest {

    @Test fun parseCommaSeparated() {
        assertEquals(null, parseCommaSeparated("xxx"))
        assertEquals(listOf(0), parseCommaSeparated("0"))
        assertEquals(listOf(0), parseCommaSeparated("0,"))
        assertEquals(listOf(0), parseCommaSeparated("0 , "))
        assertEquals(listOf(0,0), parseCommaSeparated("0,0"))
        assertEquals(listOf(0,0), parseCommaSeparated("0  ,  0"))
        assertEquals(listOf(0,0), parseCommaSeparated("0,0,x"))
        assertEquals(listOf(0,0,0), parseCommaSeparated("0,0,0  "))
    }

    @Test fun parseCommaSeparated_does_not_consume_last_comma() {
        assertEquals(
            1,
            StringWithCursor("0 , ")
                .also { it.parseCommaSeparated(false, StringWithCursor::parse0) }
                .cursor
        )

        assertEquals(
            3,
            StringWithCursor("0,0 ")
                .also { it.parseCommaSeparated(false, StringWithCursor::parse0) }
                .cursor
        )

        assertEquals(
            3,
            StringWithCursor("0,0 ,  ")
                .also { it.parseCommaSeparated(false, StringWithCursor::parse0) }
                .cursor
        )
    }

    @Test fun nextIsRangeAndAdvance() {
        assertEquals(true, StringWithCursor("-").nextIsRangeAndAdvance(false))
        assertEquals(true, StringWithCursor(" -").nextIsRangeAndAdvance(false))
        assertEquals(true, StringWithCursor("   -").nextIsRangeAndAdvance(false))
        assertEquals(false, StringWithCursor("—").nextIsRangeAndAdvance(false))

        assertEquals(true, StringWithCursor("—").nextIsRangeAndAdvance(true))
        assertEquals(true, StringWithCursor("–").nextIsRangeAndAdvance(true))
        assertEquals(false, StringWithCursor("to").nextIsRangeAndAdvance(true))
        assertEquals(false, StringWithCursor(" to").nextIsRangeAndAdvance(true))
        assertEquals(true, StringWithCursor(" to ").nextIsRangeAndAdvance(true))

        val x = StringWithCursor(" x")
        assertEquals(false, x.nextIsRangeAndAdvance(false))
        assertEquals(0, x.cursor)
    }

    @Test fun nextNumberAndAdvance() {
        val x = StringWithCursor("09a7")
        assertEquals(null, x.nextNumberAndAdvance(1))
        assertEquals("09", x.nextNumberAndAdvance(2))
        assertEquals(null, x.nextNumberAndAdvance())
        x.advance()
        assertEquals("7", x.nextNumberAndAdvance())
    }
}

private fun StringWithCursor.parse0(): Int? =
    if (nextIsAndAdvance('0')) 0 else null

private fun parseCommaSeparated(s: String) =
    StringWithCursor(s).parseCommaSeparated(false, StringWithCursor::parse0)

