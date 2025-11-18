package de.westnordost.osm_opening_hours.model

import kotlin.test.Test
import kotlin.test.assertFails

class NthTest {
    @Test fun bounds() {
        for (i in listOf(-1, 0, 6)) {
            assertFails { Nth(i) }
        }
        for (i in 1..5) {
            Nth(i)
        }
    }
}

class LastNthTest {
    @Test fun bounds() {
        for (i in listOf(-1, 0, 6)) {
            assertFails { LastNth(i) }
        }
        for (i in 1..5) {
            LastNth(i)
        }
    }
}

class NthRangeTest {
    @Test fun bounds() {
        for (i in listOf(-1, 0, 6)) {
            assertFails { NthRange(i, i+1) }
            assertFails { NthRange(i-1, i) }
        }
        assertFails { NthRange(2, 1) }
        for (i in 1..4) {
            NthRange(i, i+1)
        }
    }
}
