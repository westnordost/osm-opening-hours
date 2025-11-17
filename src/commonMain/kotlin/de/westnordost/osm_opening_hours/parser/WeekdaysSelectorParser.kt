package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.*
import kotlin.getValue

internal fun StringWithCursor.parseWeekdaySelector(lenient: Boolean): WeekdaysSelector? {
    val start = parseWeekday(lenient) ?: return null

    if (nextIsRangeAndAdvance(lenient)) {
        skipWhitespaces(lenient)
        val end = parseWeekday(lenient) ?: fail("Expected an end weekday")
        return WeekdayRange(start, end)
    }

    if (nextIsAndAdvance('[', lenient, skipWhitespaces = true)) {

        skipWhitespaces(lenient)
        val nths = parseCommaSeparated(lenient) { parseNthSelector(lenient) }.orEmpty()
        skipWhitespaces(lenient)

        if (!nextIsAndAdvance(']')) fail("Expected a ']'")

        val dayOffset = parseDayOffset(lenient)

        return SpecificWeekdays(start, nths, dayOffset ?: 0)
    }

    return start
}

internal fun StringWithCursor.parseDayOffset(lenient: Boolean): Int? {
    val initial = cursor
    skipWhitespaces(lenient)
    val op = when {
        nextIsAndAdvance('+') -> +1
        nextIsAndAdvance('-') -> -1
        else -> {
            cursor = initial
            return null
        }
    }
    skipWhitespaces(lenient)
    val days = nextNumberAndAdvance()
    if (days == null) {
        cursor = initial
        return null
    }
    skipWhitespaces(lenient)
    if (!nextIsAndAdvance("day", ignoreCase = lenient)) {
        cursor = initial
        return null
    }
    nextIsAndAdvance('s', ignoreCase = lenient)

    return op * days.toInt()
}

internal fun StringWithCursor.parseNthSelector(lenient: Boolean): NthSelector {
    val minus = nextIsAndAdvance('-')

    skipWhitespaces(lenient)

    val start = nextNumberAndAdvance(1)?.toInt() ?: fail("Expected an nth")

    val end = if (nextIsRangeAndAdvance(lenient)) {
        if (minus) fail("Negative nth not allowed in range")
        skipWhitespaces(lenient)
        nextNumberAndAdvance(1)?.toInt() ?: fail("Expected an end nth")
    } else null

    return when {
        minus ->       LastNth(start)
        end != null -> NthRange(start, end)
        else ->        Nth(start)
    }
}

internal fun StringWithCursor.parseWeekday(lenient: Boolean): Weekday? {
    return if (lenient) parseWeekdayLenient() else parseWeekdayStrict()
}

private val weekdaysMap: Map<String, Weekday> = Weekday.entries.associateBy { it.osm }
private val weekdaysMaxLength: Int = weekdaysMap.keys.maxOf { it.length }

private fun StringWithCursor.parseWeekdayStrict(): Weekday? {
    val word = getNextKeyword(weekdaysMaxLength) ?: return null
    val event = weekdaysMap[word] ?: return null
    advanceBy(word.length)
    return event
}

private val lenientWeekdaysMap: Map<String, Weekday> by lazy {
    val map = HashMap<String, Weekday>()

    // correct 2-letter abbreviations
    Weekday.entries.associateByTo(map) { it.osm.lowercase() }

    // full English names
    Weekday.entries.associateByTo(map) { it.name.lowercase() }

    // NOTE: Since we parse leniently without knowing in which language the
    // opening hours string was written, accepted abbreviations must be
    // unambiguous. Even in lenient parsing, the string must be unambiguous.
    //
    // E.g. "mar" stands for Tuesday in Spanish. If in another language "mar"
    // would stand for another weekday, we couldn't accept "mar" in lenient
    // parsing.
    //
    // So, keep that in mind when adding more languages. It is generally less
    // problematic to add languages from the same family, because it turns out
    // that the abbreviations for weekdays are often *very* similar in the same
    // family (or ones that use an own script).
    //
    // Languages included are generally the biggest language families with a
    // focus on those that are actually present in OSM data. It is expected
    // that these will be mostly where OSM (craft) mappers are most active

    val namesLists = listOf(
        // west germanic
        listOf("mo", "di", "mi", "do", "fr", "sa", "so"), // de
        listOf("ma", "di", "wo", "do", "vr", "za", "zo"), // nl
        listOf("ma", "di", "wo", "do", "vr", "sa", "so"), // af
        listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun"), // en
        // north germanic
        listOf("man", "tir", "ons", "tor", "fre", "lør", "søn"), // nb (no), da
        listOf("mån", "tys", "ons", "tor", "fre", "lau", "søn"), // nn (no)
        listOf("mån", "tis", "ons", "tors", "fre", "lör", "sön"), // sv

        // romance
        listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom"), // es
        listOf("lun", "mar", "mer", "jeu", "ven", "sam", "dim"), // fr
        listOf("lun", "mar", "mer", "gio", "ven", "sab", "dom"), // it
        listOf("lun", "mar", "mie", "joi", "vin", "sâm", "dum"), // ro
        listOf("seg", "ter", "qua", "qui", "sex", "sáb", "dom"), // pt

        // chinese, korean, japanese (short)
        listOf("月", "火", "水", "木", "金", "土", "日"), // ja
        listOf("월", "화", "수", "목", "금", "토", "일"), // ko
        listOf("周一","周二","周三","周四","周五","周六","周日"), // zh
        // chinese, korean, japanese (full) - still quite short :-)
        listOf("月曜日", "火曜日", "水曜日", "木曜日", "金曜日", "土曜日", "日曜日"), // ja
        listOf("월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일"), // ko
        listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"), // zh, yue

        // slavic
        listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс"), // ru
        listOf("пн", "вт", "ср", "чт", "пт", "сб", "нд"), // uk
        listOf("pon", "wt", "śr", "czw", "pt", "sob", "niedz"), // pl

        // arabic, hindi, urdu, bengali, telugu, tamil, persian, indonesian…
        // not really present in the data / no proper abbreviations
    )
    for (names in namesLists) {
        for (i in 0 ..< 7) map[names[i]] = Weekday.entries[i]
    }

    map
}

private val lenientWeekdaysMaxLength: Int by lazy {
    lenientWeekdaysMap.keys.maxOf { it.length }
}

private fun StringWithCursor.parseWeekdayLenient(): Weekday? {
    val word = getNextWord(lenientWeekdaysMaxLength) { it.isLetter() }?.lowercase() ?: return null
    val event = lenientWeekdaysMap[word] ?: return null
    advanceBy(word.length)
    nextIsAndAdvance('.')
    return event
}
