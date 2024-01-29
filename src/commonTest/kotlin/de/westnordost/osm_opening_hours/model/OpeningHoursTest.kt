package de.westnordost.osm_opening_hours.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFails

class OpeningHoursTest {

    @Test fun bounds() {
        OpeningHours(
            Rule(Range(), comment = "hey"),
            Rule(Range(), comment = "dude", ruleOperator = RuleOperator.Additional),
        )
        OpeningHours(
            Rule(Range(), ruleType = RuleType.Open),
            Rule(Range(), comment = "dude", ruleOperator = RuleOperator.Additional),
        )
        OpeningHours(
            Rule(Range(times = listOf(ClockTime(8)))),
            Rule(Range(), comment = "dude", ruleOperator = RuleOperator.Additional),
        )
    }

    @Test fun to_string() {
        val mo0830 = Rule(Range(
            weekdays = listOf(Weekday.Monday),
            times = listOf(ClockTime(8,30))
        ))
        val mo1600 = Rule(Range(
            weekdays = listOf(Weekday.Monday),
            times = listOf(ClockTime(16,0))
        ))
        val empty = Rule(Range())

        assertEquals(
            "Mo 08:30",
            OpeningHours(mo0830).toString()
        )
        assertEquals(
            "Mo 08:30",
            OpeningHours(empty, empty, mo0830, empty).toString()
        )
        assertEquals(
            "Mo 08:30, Mo 16:00",
            OpeningHours(mo0830, mo1600.copy(ruleOperator = RuleOperator.Additional)).toString()
        )
        assertEquals(
            "Mo 08:30; Mo 16:00",
            OpeningHours(mo0830, mo1600.copy(ruleOperator = RuleOperator.Normal)).toString()
        )
        assertEquals(
            "Mo 08:30 || Mo 16:00",
            OpeningHours(mo0830, mo1600.copy(ruleOperator = RuleOperator.Fallback)).toString()
        )
    }
}

class RuleTest {
    
    @Test fun bounds() {
        assertFails { Rule(Range(), comment = "\"Hey\"") }
        Rule(Range())
    }

    @Test fun is_empty() {
        assertTrue(Rule(Range()).isEmpty())
    }

    @Test fun to_string() {
        assertEquals(
            "Mo",
            Rule(Range(weekdays = listOf(Weekday.Monday))).toString()
        )
        assertEquals(
            "open",
            Rule(Range(), ruleType = RuleType.Open).toString()
        )
        assertEquals(
            "\"don't know\"",
            Rule(Range(), comment = "don't know").toString()
        )

        assertEquals(
            "Mo \"bla\"",
            Rule(
                selector = Range(weekdays = listOf(Weekday.Monday)),
                comment = "bla"
            ).toString()
        )
        assertEquals(
            "Mo closed",
            Rule(
                selector = Range(weekdays = listOf(Weekday.Monday)),
                ruleType = RuleType.Closed
            ).toString()
        )
        assertEquals(
            "Mo unknown \"bla\"",
            Rule(
                selector = Range(weekdays = listOf(Weekday.Monday)),
                ruleType = RuleType.Unknown,
                comment = "bla"
            ).toString()
        )
    }
}