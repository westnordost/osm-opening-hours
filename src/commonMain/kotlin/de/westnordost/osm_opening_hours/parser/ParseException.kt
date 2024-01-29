package de.westnordost.osm_opening_hours.parser

class ParseException(val cursorPos: Int, message: String?) : Exception(message) {
    override fun toString() = "At $cursorPos: $message"
}