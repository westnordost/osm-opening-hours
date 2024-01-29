package de.westnordost.osm_opening_hours.model

import kotlin.jvm.JvmInline

sealed interface YearsSelector

private const val MIN_YEAR_EXCL = 1900
private const val MAX_YEAR = 9999

/** A single [year] */
@JvmInline
value class Year(val year: Int) : YearsSelector {
    init {
        validateYear("year", year)
    }

    operator fun rangeTo(end: Year) = YearRange(this.year, end.year)
    override fun toString() = year.toString()
}

/** Since the [start] year */
data class StartingAtYear(val start: Int) : YearsSelector {
    init {
        validateYear("start", start)
    }

    override fun toString() = "$start+"
}

/** Within a range of years ([start]-[end]) with an optional interval of [step] years.
 *  E.g. between 2014-2024 but only every 2nd year */
data class YearRange(val start: Int, val end: Int, val step: Int? = null) : YearsSelector {
    init {
        validateYear("start", start)
        validateYear("end", end)
        if (step != null) require(step > 0) { "step must be a positive integer but was $step" }
        require(start <= end) { "Start year $start must be smaller or equal end year $end" }
    }

    override fun toString() = "$start-$end${if (step != null) "/$step" else ""}"
}

internal fun validateYear(name: String, value: Int) {
    require(value > MIN_YEAR_EXCL) { "$name must be greater than $MIN_YEAR_EXCL but was $value" }
    require(value <= MAX_YEAR) { "$name must be at most $MAX_YEAR but was $value" }
}
