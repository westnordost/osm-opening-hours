package de.westnordost.osm_opening_hours.model

/** Single month. The ordinal 0 is January. */
enum class Month(internal val osm: String) {
    January("Jan"),
    February("Feb"),
    March("Mar"),
    April("Apr"),
    May("May"),
    June("Jun"),
    July("Jul"),
    August("Aug"),
    September("Sep"),
    October("Oct"),
    November("Nov"),
    December("Dec");

    operator fun rangeTo(end: Month) = MonthRange(this, end)
    override fun toString() = osm
}
