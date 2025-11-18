package de.westnordost.osm_opening_hours.model

import de.westnordost.osm_opening_hours.model.Month.April
import de.westnordost.osm_opening_hours.model.Month.August
import de.westnordost.osm_opening_hours.model.Month.January
import kotlin.test.Test
import kotlin.test.assertEquals

class MonthTest {
    @Test fun operators() {
        assertEquals(MonthRange(April, August), April..August)
    }

    @Test fun to_string() {
        assertEquals("Jan", January.toString())
    }
}
