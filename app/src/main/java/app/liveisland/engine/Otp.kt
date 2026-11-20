package app.liveisland.engine

/** Finds one-time codes in notification text. Pure logic, unit-tested. */
object Otp {
    private val keyword = Regex(
        """(otp|one[- ]time|verification|verify|passcode|security code|login code|auth(?:entication)? code|\bcode\b|pin\b|कोड|ओटीपी)""",
        RegexOption.IGNORE_CASE,
    )
    private val code = Regex("""(?<![\d₹$.,/:])(\d{3}[- ]\d{3}|\d{4,8})(?![\d%]|[.,/:]\d)""")
    private val money = Regex("""(rs\.?|inr|₹|\$|usd|eur|amount|balance|bal\b)\s*$""", RegexOption.IGNORE_CASE)

    fun find(text: String): String? {
        if (!keyword.containsMatchIn(text)) return null
        for (m in code.findAll(text)) {
            val before = text.substring(0, m.range.first)
            if (money.containsMatchIn(before.takeLast(8))) continue
            val digits = m.value.filter(Char::isDigit)
            if (digits.length !in 4..8) continue
            // skip things that look like years in dates: 2026 next to a month name
            if (digits.length == 4 && digits.startsWith("20") && Regex("""(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)""", RegexOption.IGNORE_CASE).containsMatchIn(before.takeLast(12))) continue
            return digits
        }
        return null
    }
}
