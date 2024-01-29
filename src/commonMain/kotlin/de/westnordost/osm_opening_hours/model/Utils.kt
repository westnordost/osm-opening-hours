package de.westnordost.osm_opening_hours.model

import kotlin.math.abs

internal fun Sequence<Any?>.joinNonEmptyStrings(separator: String): String =
    map { it?.toString() }.filterNot { it.isNullOrEmpty() }.joinToString(separator)

internal fun dayOffsetToString(days: Int) = when (days) {
    0 -> "" // day offset must be non-zero according to the spec
    else -> {
        val sign = if (days > 0) "+" else "-"
        val name = if (abs(days) == 1) "day" else "days"
        "$sign${abs(days)} $name"
    }
}
