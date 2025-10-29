@file:Suppress("unused")

package benchmark

import de.westnordost.osm_opening_hours.model.OpeningHours
import de.westnordost.osm_opening_hours.parser.toOpeningHoursOrNull
import dev.sargunv.kotlindsv.Tsv
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.BenchmarkTimeUnit
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.TearDown
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InputRecord(
    @SerialName("?times") val times: String,
    @SerialName("?count") val count: Int,
)

class ResultRecord {
    var parsed: OpeningHours? = null
    var created: String? = null
    var valid: Boolean = false
    var pooleParsed: List<ch.poole.openinghoursparser.Rule>? = null
    var pooleCreated: String? = null
    var pooleValid: Boolean = false
}

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
open class Benchmark {
    lateinit var state: List<Pair<InputRecord, ResultRecord>>

    @Setup
    fun prepare() {
        state =
            SystemFileSystem.source(Path("../sample-data/opening_hours_counts.tsv")).buffered().use {
                source ->
                Tsv.decodeFromSource<InputRecord>(source).map { it to ResultRecord() }.toList()
            }
    }

    @Benchmark
    fun parse() = state.forEach { (input, result) ->
        input.times.toOpeningHoursOrNull(lenient = true)?.let { oh -> result.parsed = oh }
    }

    @Benchmark
    fun parseStrict() = state.forEach { (input, result) ->
        input.times.toOpeningHoursOrNull(lenient = true)?.let { _ -> result.valid = true }
    }

    @Benchmark
    fun create() = state.forEach { (_, result) -> result.created = result.parsed?.toString() }

    @Benchmark
    fun pooleParse() = state.forEach { (input, result) ->
        result.pooleParsed =
            try {
                ch.poole.openinghoursparser
                    .OpeningHoursParser(input.times.byteInputStream())
                    .rules(false)
            } catch (e: ch.poole.openinghoursparser.ParseException) {
                null
            }
    }

    @Benchmark
    fun pooleParseStrict() = state.forEach { (input, result) ->
        val rules =
            try {
                ch.poole.openinghoursparser
                    .OpeningHoursParser(input.times.byteInputStream())
                    .rules(true)
            } catch (e: ch.poole.openinghoursparser.ParseException) {
                null
            }
        if (rules != null) result.pooleValid = true
    }

    @Benchmark
    fun pooleCreate() = state.forEach { (_, result) ->
        result.pooleCreated =
            result.pooleParsed?.let {
                ch.poole.openinghoursparser.Util.rulesToOpeningHoursString(it)
            }
    }

    @TearDown
    fun teardown() {
        println("Results:")
        processResults()
        println()
        println("Poole results:")
        processPooleResults()
    }

    private fun processResults() = printReport(
        invalidParseable =
            state
                .filter { (_, result) -> result.parsed != null && !result.valid }
                .map { (input, _) -> input }
                .also { outputToFile(it, "invalid_but_unambiguous_opening_hours.tsv") },
        invalidNonParseable =
            state
                .filter { (_, result) -> result.parsed == null }
                .map { (input, _) -> input }
                .also { outputToFile(it, "invalid_opening_hours.tsv") },
        canonical =
            state
                .filter { (input, result) -> input.times == result.created }
                .map { (input, _) -> input },
    )

    private fun outputToFile(records: List<InputRecord>, name: String) {
        SystemFileSystem.sink(Path("../sample-data/$name")).buffered().use { sink ->
            Tsv.encodeToSink(records.sortedBy { -it.count }.asSequence(), sink)
        }
    }

    private fun processPooleResults() = printReport(
        invalidParseable =
            state
                .filter { (_, result) -> result.pooleParsed != null && !result.pooleValid }
                .map { (input, _) -> input },
        invalidNonParseable =
            state
                .filter { (_, result) -> result.pooleParsed == null }
                .map { (input, _) -> input },
        canonical =
            state
                .filter { (input, result) -> input.times == result.pooleCreated }
                .map { (input, _) -> input },
    )

    private fun printReport(
        invalidParseable: List<InputRecord>,
        invalidNonParseable: List<InputRecord>,
        canonical: List<InputRecord>,
    ) {
        val sum = state.fold(0) { acc, (input, _) -> acc + input.count }

        val sumInvalidParseable = invalidParseable.fold(0) { acc, input -> acc + input.count }
        val sumInvalidNonParseable = invalidNonParseable.fold(0) { acc, input -> acc + input.count }
        val sumCanonical = canonical.fold(0) { acc, input -> acc + input.count }

        val sumValid = sum - sumInvalidNonParseable - sumInvalidParseable
        val sumParseable = sum - sumInvalidNonParseable

        println("${sumValid.toDouble() / sum * 100}% are valid")
        println("${sumParseable.toDouble() / sum * 100}% can be parsed")
        println("${sumCanonical.toDouble() / sum * 100}% are in their canonical form")
    }
}
