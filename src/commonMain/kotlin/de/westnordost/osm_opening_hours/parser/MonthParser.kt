package de.westnordost.osm_opening_hours.parser

import de.westnordost.osm_opening_hours.model.Month

private val monthsMap: Map<String, Month> by lazy {
    Month.entries.associateByTo(HashMap()) { it.osm }
}
private val monthsMaxLength: Int by lazy { monthsMap.keys.maxOf { it.length } }

private val lenientMonthsMap: Map<String, Month> by lazy {
    val map = HashMap<String, Month>()

    // correct 3-letter abbreviations
    Month.entries.associateByTo(map) { it.osm.lowercase() }

    // full English names
    Month.entries.associateByTo(map) { it.name.lowercase() }

    // NOTE: Since we parse leniently without knowing in which language the
    // opening hours string was written, accepted abbreviations must be
    // unambiguous in all languages. Even in lenient parsing, the string must
    // be unambiguous.
    //
    // E.g. "mar" stands for Tuesday in Spanish, but also for "March" in
    // English (and Spanish, too!). So, we can't accept "mar" as an
    // abbreviation for Tuesday.

    val namesLists = listOf(
        // germanic
        listOf("jan", "feb", "mär", "apr", "mai", "jun", "jul", "aug", "sep", "okt", "nov", "dez"), // de
        listOf("jan", "feb", "mrt", "apr", "mei", "jun", "jul", "aug", "sep", "okt", "nov", "dec"), // nl
        listOf("jan", "feb", "mrt", "apr", "mei", "jun", "jul", "aug", "sep", "okt", "nov", "des"), // af
        listOf("jan", "feb", "mar", "apr", "maj", "jun", "jul", "aug", "sep", "okt", "nov", "dec"), // da
        listOf("jan", "feb", "mar", "apr", "mai", "jun", "jul", "aug", "sep", "okt", "nov", "des"), // nb
        listOf("jan", "feb", "mars", "apr", "mai", "juni", "juli", "aug", "sep", "okt", "nov", "des"), // nn
        listOf("jan", "feb", "mars", "apr", "maj", "juni", "juli", "aug", "sep", "okt", "nov", "dec"), // sv

        // romance
        listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sept", "oct", "nov", "dic"), // es
        listOf("gen", "feb", "mar", "apr", "mag", "giu", "lug", "ago", "set", "ott", "nov", "dic"), // it
        listOf("ian", "feb", "mar", "apr", "mai", "iun", "iul", "aug", "sept", "oct", "nov", "dec"), // ro
        listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"), // pt
        listOf("janv", "févr", "mars", "avr", "mai", "juin", "juil", "août", "sept", "oct", "nov", "déc"), // fr

        // slavic
        listOf("янв", "февр", "март", "апр", "май", "июнь", "июль", "авг", "сент", "окт", "нояб", "дек"), // ru
        listOf("січ", "лют", "бер", "кві", "тра", "чер", "лип", "сер", "вер", "жов", "лис", "гру"), // uk
    )
    for (names in namesLists) {
        for (i in 0 ..< 12) map[names[i]] = Month.entries[i]
    }

    map
}

private val lenientMonthsMaxLength: Int by lazy {
    lenientMonthsMap.keys.maxOf { it.length }
}

internal fun StringWithCursor.parseMonth(lenient: Boolean): Month? {
    return if (lenient) parseMonthLenient() else parseMonthStrict()
}

private fun StringWithCursor.parseMonthStrict(): Month? {
    val word = getNextKeyword(monthsMaxLength) ?: return null
    val month = monthsMap[word] ?: return null
    advanceBy(word.length)
    return month
}

private fun StringWithCursor.parseMonthLenient(): Month? {
    val word = getNextWord(lenientMonthsMaxLength) { it.isLetter() }?.lowercase() ?: return null
    val event = lenientMonthsMap[word] ?: return null
    advanceBy(word.length)
    nextIsAndAdvance('.')
    return event
}
