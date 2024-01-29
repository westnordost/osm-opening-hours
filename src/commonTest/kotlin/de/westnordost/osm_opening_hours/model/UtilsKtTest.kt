package de.westnordost.osm_opening_hours.model

import de.westnordost.osm_opening_hours.model.dayOffsetToString
import de.westnordost.osm_opening_hours.model.joinNonEmptyStrings
import kotlin.test.Test
import kotlin.test.assertEquals

class UtilKtTest {
    @Test fun dayOffset() {
        assertEquals("", dayOffsetToString(0))
        assertEquals("-1 day", dayOffsetToString(-1))
        assertEquals("-2 days", dayOffsetToString(-2))
        assertEquals("+1 day", dayOffsetToString(+1))
        assertEquals("+2 days", dayOffsetToString(+2))
    }

    @Test fun joinNonEmptyStrings() {
        assertEquals(
            "hey what's up",
            sequenceOf("hey", null, "", "what's", "up").joinNonEmptyStrings(" ")
        )
    }
}
