package de.westnordost.osm_opening_hours

import de.westnordost.osm_opening_hours.parser.toOpeningHoursOrNull
import kotlinx.io.readString
import kotlin.test.Test
import kotlin.test.assertEquals

data class OpeningHoursRow(val hours: String, val count: Int)

class IntegrationTest {

    @Test fun testRealData() {
        val rows = useResource("opening_hours_counts.tsv") { it.readString() }
            .lineSequence()
            .drop(1)
            .filterNot { it.isBlank() }
            .map { line ->
                val t = line.lastIndexOf('\t')
                val oh = line.substring(1, t - 1)
                val count = line.substring(t + 1).toInt()
                OpeningHoursRow(oh, count)
            }

        for ((i, row) in rows.withIndex()) {
            val hours = row.hours
            val strict = hours.toOpeningHoursOrNull(lenient = false)
            val lenient = hours.toOpeningHoursOrNull(lenient = true)

            if (strict != null) {
                // when opening hours have been parsed successfully in strict mode,
                // they must also have the same result in lenient mode
                assertEquals(strict, lenient,
                    """
                    At line $i: Result of strict and lenient parsing must be the same
                    String: $hours

                    """.trimIndent()
                )
            }
        }
    }

}
