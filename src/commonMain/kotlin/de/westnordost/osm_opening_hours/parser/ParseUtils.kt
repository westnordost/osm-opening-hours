package de.westnordost.osm_opening_hours.parser

/** Returns a comma separated list of items parsed in the [block] or null if the list would be
 *  empty. Note that the cursor stops after the last successfully parsed item, e.g. for
 *  "a,b,c," (given that a, b and c can be parsed), it stops after "c". */
internal fun <T> StringWithCursor.parseCommaSeparated(
    lenient: Boolean,
    block: StringWithCursor.() -> T?
): List<T>? {
    val list = ArrayList<T>()
    var cursorAfterLastItem = cursor

    do {
        if (!list.isEmpty()) skipWhitespaces(lenient)
        val item = block() ?: break
        list.add(item)
        cursorAfterLastItem = cursor
    } while (nextIsCommaAndAdvance(lenient = lenient, skipWhitespaces = true))

    cursor = cursorAfterLastItem

    return list.takeIf { it.isNotEmpty() }
}

/** Returns true if next character is a comma. If lenient, any kind of comma: normal,
 *  full-width, Chinese comma, ...
 *  If [skipWhitespaces], the function will also consume any whitespaces in front of the character.*/
internal fun StringWithCursor.nextIsCommaAndAdvance(lenient: Boolean, skipWhitespaces: Boolean = false): Boolean {
    val ws = if (skipWhitespaces) skipWhitespaces(lenient) else 0

    val isComma = if (!lenient) {
        nextIsAndAdvance(',')
    } else {
        nextIsAndAdvance { it == ',' || it == '，' || it == '、' || it == '､' || it == '﹑' } != null
    }

    if (isComma) {
        return true
    } else {
        retreatBy(ws)
        return false
    }
}

/** Returns true if next character is a colon. If lenient, any kind of colon: normal, full-width comma, ...
 *  If [skipWhitespaces], the function will also consume any whitespaces in front of the character. */
internal fun StringWithCursor.nextIsColonAndAdvance(lenient: Boolean, skipWhitespaces: Boolean = false): Boolean {
    val ws = if (skipWhitespaces) skipWhitespaces(lenient) else 0

    val isComma = if (!lenient) {
        nextIsAndAdvance(':')
    } else {
        nextIsAndAdvance { it == ':' || it == '：' } != null
    }

    if (isComma) {
        return true
    } else {
        retreatBy(ws)
        return false
    }
}

/** Returns true if next character (after whitespaces) is a character indicating a range and
 *  advances past that character */
internal fun StringWithCursor.nextIsRangeAndAdvance(lenient: Boolean): Boolean {
    val ws = skipWhitespaces(lenient)
    val isRange = if (!lenient) {
        nextIsAndAdvance('-')
    } else {
        nextIsAndAdvance('-') || // normal minus
        nextIsAndAdvance('–') || // en dash (thousands of usages!)
        nextIsAndAdvance('—') || // em dash
        nextIsAndAdvance('～') || // wave dash (used in Japanese)
        nextIsAndAdvance('~') || // probably used instead of wave dash
        ws > 0 && nextIsAndAdvance("to ", true) // (thousands of usages!)
        // not including other languages than English here because usually, there is more wrong
        // with the syntax of the string if they don't even try to write in English
    }
    if (isRange) {
        return true
    } else {
        retreatBy(ws)
        return false
    }
}

/** Advance the cursor until there is no whitespace and return the number of
 *  characters advanced */
internal fun StringWithCursor.skipWhitespaces(lenient: Boolean): Int {
    return if (lenient) advanceWhile { it.isWhitespace() } else advanceWhile { it == ' ' }
}

/** Retreat the cursor until there is no whitespace and return the number of
 *  characters advanced */
internal fun StringWithCursor.retreatWhitespaces(lenient: Boolean): Int {
    return if (lenient) retreatWhile { it.isWhitespace() } else retreatWhile { it == ' ' }
}

internal fun StringWithCursor.getNextKeyword(maxLength: Int? = null): String? {
    return getNextWord(maxLength) { it in 'a'..'z' || it in 'A' .. 'Z'}
}

/** Advances the cursor if [c] is the next character at the cursor. If [skipWhitespaces],
 *  the function will also consume any whitespaces in front of the character.
 *  @return whether the next character was [c] */
internal fun StringWithCursor.nextIsAndAdvance(
    c: Char, lenient: Boolean, skipWhitespaces: Boolean, ignoreCase: Boolean = false,
): Boolean {
    val ws = if (skipWhitespaces) skipWhitespaces(lenient) else 0
    if (nextIsAndAdvance(c, ignoreCase)) {
        return true
    } else {
        retreatBy(ws)
        return false
    }
}

/** @return the next string that contains only digits of the given [maxLength]. Returns null if
 *  the number is either longer than that or there is no word at this position. If not null
 *  is returned, will also advance the cursor accordingly */
internal fun StringWithCursor.nextNumberAndAdvance(lenient: Boolean, maxLength: Int? = null): String? {
    val number =
        if (!lenient) getNextWord(maxLength) { it in '0'..'9' }
        else getNextWord(maxLength) { it.isDigit() }
    if (number == null) return null
    advanceBy(number.length)
    return number
}

internal fun StringWithCursor.fail(message: String): Nothing =
    throw ParseException(cursor, message)

