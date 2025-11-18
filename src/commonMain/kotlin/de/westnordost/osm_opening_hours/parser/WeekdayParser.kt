package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.Weekday

private val weekdaysMap: Map<String, Weekday> by lazy {
    Weekday.entries.associateByTo(HashMap()) { it.osm }
}
private val weekdaysMaxLength: Int by lazy { weekdaysMap.keys.maxOf { it.length } }

private val lenientWeekdaysMap: Map<String, Weekday> by lazy {
    val map = HashMap<String, Weekday>()

    // correct 2-letter abbreviations
    Weekday.entries.associateByTo(map) { it.osm.lowercase() }

    // full English names
    Weekday.entries.associateByTo(map) { it.name.lowercase() }

    // NOTE: Since we parse leniently without knowing in which language the
    // opening hours string was written, accepted abbreviations must be
    // unambiguous in all languages. Even in lenient parsing, the string must
    // be unambiguous.
    //
    // E.g. "mar" stands for Tuesday in Spanish, but also for "March" in
    // English (and Spanish, too!). So, we can't accept "mar" as an
    // abbreviation for Tuesday.

    val namesLists = listOf(
        // west germanic
        listOf("mo", "di", "mi", "do", "fr", "sa", "so"), // de
        listOf("ma", "di", "wo", "do", "vr", "za", "zo"), // nl
        listOf("ma", "di", "wo", "do", "vr", "sa", "so"), // af
        listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun"), // en
        listOf("man", "tir", "ons", "tor", "fre", "lør", "søn"), // nb (no), da
        listOf("mån", "tys", "ons", "tor", "fre", "lau", "søn"), // nn (no)
        listOf("mån", "tis", "ons", "tors", "fre", "lör", "sön"), // sv

        // romance (see above comment for "tue" instead of "mar")
        listOf("lun", "tue", "mié", "jue", "vie", "sáb", "dom"), // es
        listOf("lun", "tue", "mer", "jeu", "ven", "sam", "dim"), // fr
        listOf("lun", "tue", "mer", "gio", "ven", "sab", "dom"), // it
        listOf("lun", "tue", "mie", "joi", "vin", "sâm", "dum"), // ro
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

internal fun StringWithCursor.parseWeekday(lenient: Boolean): Weekday? {
    return if (lenient) parseWeekdayLenient() else parseWeekdayStrict()
}

private fun StringWithCursor.parseWeekdayStrict(): Weekday? {
    val word = getNextKeyword(weekdaysMaxLength) ?: return null
    val event = weekdaysMap[word] ?: return null
    advanceBy(word.length)
    return event
}

private fun StringWithCursor.parseWeekdayLenient(): Weekday? {
    val word = getNextWord(lenientWeekdaysMaxLength) { it.isLetter() }?.lowercase() ?: return null
    val event = lenientWeekdaysMap[word] ?: return null
    advanceBy(word.length)
    nextIsAndAdvance('.')
    return event
}
