package de.westnordost.osm_opening_hours.model

sealed interface HolidaySelector

/** At a [holiday] with [dayOffset], e.g. 1 day before a public holiday */
data class HolidayWithOffset(val holiday: Holiday, val dayOffset: Int): HolidaySelector {
    override fun toString() = "$holiday ${dayOffsetToString(dayOffset)}"
}

enum class Holiday(internal val osm: String): HolidaySelector {
    PublicHoliday("PH"),
    SchoolHoliday("SH");

    override fun toString() = osm
}