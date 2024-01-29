package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class OpeningHoursParserKtTest {
    @Test fun parseOpeningHours() {
        assertFails { parse("") }
        assertFails { parse("   ") }
        assertFails { parse(";;;") }
        assertFails { parse("Mo Jan") }
        assertFails { parse("something") }

        assertEquals(
            OpeningHours(
                Rule(Range()),
                Rule(Range(years = listOf(Year(1999))))
            ),
            parse(";1999")
        )

        assertEquals(
            OpeningHours(
                Rule(Range(times = listOf(ClockTime(18)))),
                Rule(Range(times = listOf(ClockTime(20))), ruleOperator = RuleOperator.Normal)
            ),
            parse("18:00;20:00")
        )

        assertEquals(
            OpeningHours(
                Rule(Range(times = listOf(ClockTime(18)))),
                Rule(
                    selector = Range(listOf(Weekday.Monday), listOf(ClockTime(20))),
                    ruleOperator = RuleOperator.Additional
                )
            ),
            parse("18:00,Mo 20:00")
        )

        assertEquals(
            OpeningHours(Rule(Range(times = listOf(ClockTime(18), ClockTime(20))))),
            parse("18:00,20:00")
        )

        assertEquals(
            OpeningHours(
                Rule(Range(times = listOf(ClockTime(18)))),
                Rule(Range(times = listOf(ClockTime(20))), ruleOperator = RuleOperator.Fallback)
            ),
            parse("18:00||20:00")
        )

        assertEquals(
            OpeningHours(
                Rule(Range(times = listOf(ClockTime(18)))),
                Rule(Range(times = listOf(ClockTime(20))), ruleOperator = RuleOperator.Normal)
            ),
            parse("  18:00  ;  20:00  ")
        )

        assertEquals(
            OpeningHours(Rule(Range(times = listOf(ClockTime(18))), ruleType = RuleType.Open)),
            parse("18:00open")
        )

        assertEquals(
            OpeningHours(Rule(Range(times = listOf(ClockTime(18))), ruleType = RuleType.Closed)),
            parse("18:00 closed")
        )

        assertEquals(
            OpeningHours(Rule(Range(times = listOf(ClockTime(18))), comment = "yeah")),
            parse("18:00 \"yeah\"")
        )

        assertEquals(
            OpeningHours(Rule(
                selector = Range(times = listOf(ClockTime(18))),
                ruleType = RuleType.Closed,
                comment = "yeah"
            )),
            parse("18:00 closed \"yeah\"")
        )

        assertEquals(
            OpeningHours(Rule(
                selector = Range(times = listOf(ClockTime(18))),
                ruleType = RuleType.Closed,
                comment = "yeah"
            )),
            parse("18:00closed\"yeah\"")
        )
    }


    @Test fun parseRuleType() {
        assertEquals(RuleType.Open, parseRuleType("open"))
        assertEquals(RuleType.Closed, parseRuleType("closed"))
        assertEquals(null, parseRuleType(""))
        assertEquals(null, parseRuleType("something"))
        assertEquals(null, parseRuleType("OPEN"))
    }

    @Test fun parseRuleType_lenient() {
        assertEquals(RuleType.Open, parseRuleType("OPEN", true))
    }
}

private fun parse(str: String, lenient: Boolean = false) =
    str.toOpeningHours(lenient)

private fun parseRuleType(s: String, lenient: Boolean = false) =
    StringWithCursor(s).parseRuleType(lenient)
