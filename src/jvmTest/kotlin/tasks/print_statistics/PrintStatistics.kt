package tasks.print_statistics

import de.westnordost.osm_opening_hours.parser.toOpeningHoursOrNull
import okio.FileSystem
import okio.Path.Companion.toPath
import java.io.ByteArrayInputStream
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.TimeSource

fun main() {
    val file = "./src/jvmTest/resources/opening_hours_counts.tsv"

    val lines = FileSystem.SYSTEM.read(file.toPath()) { readUtf8() }
        .lineSequence()
        .drop(1)
        .filterNot { it.isBlank() }
        .map { line ->
            val t = line.lastIndexOf('\t')
            val oh = line.substring(1, t - 1)
            val count = line.substring(t + 1).toInt()
            oh to count
        }
        .toList()
        .asReversed()

    val timeSource = TimeSource.Monotonic
    var total = 0L

    var parseTime = Duration.ZERO
    var parseStrictTime = Duration.ZERO
    var createTime = Duration.ZERO
    var parsed = 0L
    var valid = 0L
    var canonical = 0L

    var pooleParseTime = Duration.ZERO
    var pooleParseStrictTime = Duration.ZERO
    var pooleCreateTime = Duration.ZERO

    var pooleParsed = 0L
    var pooleValid = 0L
    var pooleCanonical = 0L

    for ((oh, count) in lines) {
        var pooleHoursString: String? = null
        var hoursString: String? = null

        var mark = timeSource.markNow()
        val pooleHours = pooleParse(oh, strict = false)
        pooleParseTime += (timeSource.markNow() - mark) * count

        if (pooleHours != null) {
            pooleParsed += count

            mark = timeSource.markNow()
            pooleHoursString = pooleCreate(pooleHours)
            pooleCreateTime += (timeSource.markNow() - mark) * count

            if (pooleHoursString == oh) {
                pooleCanonical += count
            }
        }

        mark = timeSource.markNow()
        if (pooleParse(oh, strict = true) != null) {
            pooleValid += count
        }
        pooleParseStrictTime += (timeSource.markNow() - mark) * count

        mark = timeSource.markNow()
        val hours = oh.toOpeningHoursOrNull(lenient = true)
        parseTime += (timeSource.markNow() - mark) * count
        if (hours != null) {
            mark = timeSource.markNow()
            hoursString = hours.toString()
            createTime += (timeSource.markNow() - mark) * count

            parsed += count
            if (hoursString == oh) {
                canonical += count
            }
        }

        mark = timeSource.markNow()
        val strictHours = oh.toOpeningHoursOrNull(lenient = false)
        parseStrictTime += (timeSource.markNow() - mark) * count
        if (strictHours != null) {
            valid += count
        }

        total += count
    }

    println("Parsed $total opening hours strings.")
    println()
    println("ch.poole.openinghoursparser: ")
    print("${(100.0 * pooleValid / total).digits(2)}% are valid, ")
    print("${(100.0 * pooleParsed  / total).digits(2)}% can be parsed, ")
    print("${(100.0 * pooleCanonical  / total).digits(2)}% are in canonical form ")
    println()
    print("Average parse times: ")
    print("${(1.0 * pooleParseStrictTime.inWholeMicroseconds / total).digits(2)}μs in strict mode, ")
    print("${(1.0 * pooleParseTime.inWholeMicroseconds / total).digits(2)}μs in lenient mode.")
    println()
    print("Average creation time: ")
    print("${(1.0 * pooleCreateTime.inWholeMicroseconds / total).digits(2)}μs")
    println()
    println()
    println("de.westnordost.osm_opening_hours: ")
    print("${(100.0 * valid / total).digits(2)}% are valid, ")
    print("${(100.0 * parsed  / total).digits(2)}% can be parsed, ")
    print("${(100.0 * canonical  / total).digits(2)}% are in canonical form ")
    println()
    print("Average parse times: ")
    print("${(1.0 * parseStrictTime.inWholeMicroseconds / total).digits(2)}μs in strict mode, ")
    print("${(1.0 * parseTime.inWholeMicroseconds / total).digits(2)}μs in lenient mode.")
    println()
    print("Average creation time: ")
    print("${(1.0 * createTime.inWholeMicroseconds / total).digits(2)}μs")
    println()
    println()
}

private fun pooleParse(openingHours: String, strict: Boolean) =
    try {
        ch.poole.openinghoursparser.OpeningHoursParser(ByteArrayInputStream(openingHours.toByteArray())).rules(strict)
    } catch (e: ch.poole.openinghoursparser.ParseException) {
        null
    }

private fun pooleCreate(rules: List<ch.poole.openinghoursparser.Rule>) =
    ch.poole.openinghoursparser.Util.rulesToOpeningHoursString(rules)

private fun Double.digits(d: Int) = "%.${d}f".format(Locale.US, this)
