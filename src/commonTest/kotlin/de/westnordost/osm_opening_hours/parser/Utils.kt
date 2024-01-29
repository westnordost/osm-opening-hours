package de.westnordost.osm_opening_hours.parser

import kotlin.test.assertEquals

internal fun verifyConsumption(
    string: String,
    expectCursorPosAt: Int,
    block: StringWithCursor.() -> Any?
) {
    val s = StringWithCursor("$string ")
    s.block()
    assertEquals(expectCursorPosAt, s.cursor)
}

internal fun verifyConsumption(string: String, block: StringWithCursor.() -> Any?) {
    verifyConsumption(string, string.length, block)
}

internal fun verifyConsumption(
    string: String,
    lenient: Boolean,
    block: StringWithCursor.(Boolean) -> Any?
) {
    verifyConsumption(string, lenient, string.length, block)
}

internal fun verifyConsumption(
    string: String,
    lenient: Boolean,
    expectCursorPosAt: Int = string.length,
    block: StringWithCursor.(Boolean) -> Any?
) {
    val s = StringWithCursor("$string ")
    s.block(lenient)
    assertEquals(expectCursorPosAt, s.cursor)
}