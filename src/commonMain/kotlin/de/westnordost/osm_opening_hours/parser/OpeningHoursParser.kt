package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.*

/** Parse this string as OSM opening hours according to the
 *  [specification](https://wiki.openstreetmap.org/wiki/Key:opening_hours/specification).
 *
 *  Note that the specification also covers defining points in time (used in for example for the tag
 *  key `collection_times`). You can check whether a parsed [OpeningHours] contains points in time
 *  via [OpeningHours.containsTimePoints]
 *
 *  @param lenient Whether to accept certain unambiguous syntax variations that are not documented
 *                 in the specification as valid
 *  @throws ParseException when this string cannot be parsed as opening hours
 *  @return parsed opening hours
 *  */
fun String.toOpeningHours(lenient: Boolean = false): OpeningHours =
    StringWithCursor(this).parseOpeningHours(lenient)

/** Parse this string as OSM opening hours according to the
 *  [specification](https://wiki.openstreetmap.org/wiki/Key:opening_hours/specification).
 *
 *  Note that the specification also covers defining points in time (used in for example for the tag
 *  key `collection_times`). You can check whether a parsed [OpeningHours] contains points in time
 *  via [OpeningHours.containsTimePoints]
 *
 *  @param lenient Whether to accept certain unambiguous syntax variations that are not documented
 *                 in the specification as valid
 *  @return parsed opening hours or null if this cannot be parsed as opening hours
 *  */
fun String.toOpeningHoursOrNull(lenient: Boolean = false): OpeningHours? =
    try { toOpeningHours(lenient) } catch (e: ParseException) { null }

private fun StringWithCursor.parseOpeningHours(lenient: Boolean): OpeningHours {
    try {
        val rules = ArrayList<Rule>()
        var ruleOperator = RuleOperator.Normal
        while (true) {
            skipWhitespaces(lenient)
            rules.add(parseRule(ruleOperator, lenient))
            skipWhitespaces(lenient)
            if (isAtEnd()) break
            ruleOperator = parseRuleOperator() ?: fail("Expected rule separator")
        }
        // don't allow completely empty rules
        if (rules.all { it.isEmpty() }) fail("Only empty rules are not allowed")

        return OpeningHours(rules)
    } catch (e: IllegalArgumentException) {
        throw ParseException(cursor, e.message)
    }
}

private fun StringWithCursor.parseRuleOperator(): RuleOperator? {
    return when {
        nextIsAndAdvance(';') -> RuleOperator.Normal
        nextIsAndAdvance(',') -> RuleOperator.Additional
        nextIsAndAdvance("||") -> RuleOperator.Fallback
        else -> null
    }
}

private fun StringWithCursor.parseRule(ruleOperator: RuleOperator, lenient: Boolean): Rule {
    val selector = parseSelector(lenient)
    skipWhitespaces(lenient)
    val mode = parseRuleType(lenient)
    skipWhitespaces(lenient)
    val comment = parseComment()
    retreatWhitespaces(lenient)
    return Rule(selector, mode, comment, ruleOperator)
}

private val ruleTypeMap: Map<String, RuleType> =
    RuleType.entries.associateBy { it.osm }

private val selectorModeMaxLength: Int = ruleTypeMap.keys.maxOf { it.length }

internal fun StringWithCursor.parseRuleType(lenient: Boolean): RuleType? {
    var word = getNextKeyword(selectorModeMaxLength) ?: return null
    if (lenient) word = word.lowercase()
    val event = ruleTypeMap[word] ?: return null
    advanceBy(word.length)
    return event
}