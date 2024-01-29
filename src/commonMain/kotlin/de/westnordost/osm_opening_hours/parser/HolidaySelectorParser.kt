package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.Holiday
import de.westnordost.osm_opening_hours.model.HolidaySelector
import de.westnordost.osm_opening_hours.model.HolidayWithOffset

internal fun StringWithCursor.parseHolidaySelector(lenient: Boolean): HolidaySelector? {
    val holiday = parseHoliday(lenient) ?: return null
    val dayOffset = parseDayOffset(lenient)
    return if (dayOffset != null) HolidayWithOffset(holiday, dayOffset) else holiday
}

private fun StringWithCursor.parseHoliday(lenient: Boolean): Holiday? {
    return Holiday.entries.find { nextIsAndAdvance(it.osm, lenient) }
}