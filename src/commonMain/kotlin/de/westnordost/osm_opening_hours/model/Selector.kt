package de.westnordost.osm_opening_hours.model


sealed interface Selector {
    fun isEmpty(): Boolean
    /** Whether this selector contains points in time, e.g.`Mo-Fr 08:00` */
    fun containsTimePoints(): Boolean
    /** Whether this selector contains time spans, e.g.`Mo-Fr 08:00-12:00` or e.g. `Jan-Feb` */
    fun containsTimeSpans(): Boolean
}

internal const val WEEK = "week"

internal const val TWENTY_FOUR_SEVEN = "24/7"

/** Always */
data object TwentyFourSeven : Selector {
    override fun toString() = TWENTY_FOUR_SEVEN
    override fun isEmpty() = false
    override fun containsTimePoints() = false
    override fun containsTimeSpans() = true
}

/** At a certain time of year(s), months, weeks, weekdays, times.
 *
 * [text] can be set *instead of* [years], [months] and [weeks] to define a wide range in prose,
 * for example "'during Ramadan': Mo-Fr 20:00-22:00".
 *
 * If [isRestrictedByHolidays] is `true`, it means its only those weekdays *on* the given holidays,
 * e.g. Mondays that are school holidays. If `false`, it's on those weekdays
 * and holidays, e.g. Sundays and public holidays.
 * */
data class Range(
    val years: List<YearsSelector>? = null,
    val months: List<MonthsOrDateSelector>? = null,
    val weeks: List<WeeksSelector>? = null,
    val text: String? = null,
    val useSeparatorForReadability: Boolean? = null,
    val weekdays: List<WeekdaysSelector>? = null,
    val holidays: List<HolidaySelector>? = null,
    val isRestrictedByHolidays: Boolean = false,
    val times: List<TimesSelector>? = null,
) : Selector {

    /** Convenience constructor for weekdays plus times */
    constructor(weekdays: List<WeekdaysSelector>, times: List<TimesSelector>)
        : this(null, null, null, null, null, weekdays, null, false, times)

    init {
        if (text != null) {
            require(years == null && months == null && weeks == null) {
                "text may not be set at the same time as years, months or weeks"
            }
            require(!text.contains("\"")) { "Text must not contain a '\"' but it did: '$text'" }
        }
    }

    override fun containsTimePoints() = times?.any { it is TimePointsSelector } == true
    override fun containsTimeSpans() = !isEmpty() && times?.all { it is TimePointsSelector } != true

    override fun toString(): String {
        val wideRange =
            if (text != null) "\"$text\""
            else sequenceOf(
                years?.joinToString(","),
                months?.joinToString(","),
                weeks?.let { "$WEEK ${it.joinToString(",")}" },
            ).joinNonEmptyStrings(" ")

        val weekdaysAndHolidaysStr =
            if (isRestrictedByHolidays)
                sequenceOf(holidays, weekdays).map { it?.joinToString(",") }.joinNonEmptyStrings(" ")
            else
                sequenceOf(weekdays, holidays).map { it?.joinToString(",") }.joinNonEmptyStrings(",")

        val timesStr = times?.joinToString(",").orEmpty()

        val useSeparatorForReadability =
            // separator is mandatory when text is defined
            text != null
            // otherwise user choice (if chosen)
            || useSeparatorForReadability
                // default behavior: if both wide range and weekdays+times are set
                ?: (wideRange.isNotEmpty() && (weekdaysAndHolidaysStr.isNotEmpty() || timesStr.isNotEmpty()))

        val wideRangeStr = if (useSeparatorForReadability) "$wideRange:" else wideRange

        return sequenceOf(
            wideRangeStr,
            weekdaysAndHolidaysStr,
            times?.joinToString(",")
        ).joinNonEmptyStrings(" ")
    }

    override fun isEmpty(): Boolean =
        years.isNullOrEmpty() &&
        months.isNullOrEmpty() &&
        weeks.isNullOrEmpty() &&
        text == null &&
        weekdays.isNullOrEmpty() &&
        holidays.isNullOrEmpty() &&
        times.isNullOrEmpty()
}
