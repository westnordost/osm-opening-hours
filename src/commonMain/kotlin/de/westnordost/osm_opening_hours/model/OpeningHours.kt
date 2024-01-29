package de.westnordost.osm_opening_hours.model

/** Model for opening hours according to the specification at
 *  https://wiki.openstreetmap.org/wiki/Key:opening_hours/specification */
data class OpeningHours(val rules: List<Rule>) {
    constructor(vararg rules: Rule) : this(rules.toList())

    /** Whether these opening hours contain points in time, e.g. `Mo-Fr 08:00` */
    fun containsTimePoints(): Boolean =
        rules.any { it.selector.containsTimePoints() }

    /** Whether these opening hours contain time spans, e.g. `Mo-Fr 08:00-12:00` or e.g. `Jan-Feb` */
    fun containsTimeSpans(): Boolean =
        rules.any { it.selector.containsTimeSpans() }

    override fun toString(): String {
        val buffer = StringBuilder()

        var pending: RuleOperator? = null
        var previousRule: Rule? = null
        for (rule in rules) {
            val op = rule.ruleOperator
            if (rule.isEmpty()) {
                if (pending == null || op != RuleOperator.Additional) pending = op
                continue
            }
            if (previousRule != null) {
                val sep = if (pending != null && op == RuleOperator.Additional) pending else op

                if (sep == RuleOperator.Additional && previousRule.noAdditionalRuleMayFollow()) {
                    buffer.append(" open")
                }

                buffer.append(sep)
            }
            pending = null
            buffer.append(rule)
            previousRule = rule
        }

        return buffer.toString()
    }
}

/** Specifies the times via [selector] at which it is open/closed/unknown (see [ruleType]) with
 *  optional [comment].
 *  [ruleOperator] determines whether this rule should add to, overwrite the previous rule etc. */
data class Rule(
    val selector: Selector,
    val ruleType: RuleType? = null,
    val comment: String? = null,
    val ruleOperator: RuleOperator = RuleOperator.Normal
) {
    init {
        require(comment == null || !comment.contains("\"")) {
            "Comment must not contain a '\"' but it did: '$comment'"
        }
    }

    internal fun noAdditionalRuleMayFollow(): Boolean =
        comment == null && ruleType == null && when(selector) {
            is Range -> selector.times.isNullOrEmpty()
            TwentyFourSeven -> true
        }

    fun isEmpty(): Boolean =
        selector.isEmpty() && ruleType == null && comment == null

    override fun toString() =
        sequenceOf(selector, ruleType, comment?.let { "\"$it\"" }).joinNonEmptyStrings(" ")
}

/** Specifies what the selector specifies: The times when it is open, or closed, or ... */
enum class RuleType(internal val osm: String) {
    /** Selector specifies times when it is open  */
    Open("open"),
    /** Selector specifies times when it is closed  */
    Closed("closed"),
    /** (Same as [Closed]) */
    Off("off"),
    /** Selector specifies times, but it is unknown whether they are for open, closed or something
     *  else. (A comment is recommended but not required) */
    Unknown("unknown");

    override fun toString() = osm
}

enum class RuleOperator(internal val osm: String) {
    Normal("; "),
    Additional(", "),
    Fallback(" || ");

    override fun toString() = osm
}
