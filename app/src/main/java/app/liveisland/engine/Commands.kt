package app.liveisland.engine

import app.liveisland.island.Glyph

/** A suggestion row in the command bar. */
data class Suggestion(val title: String, val hint: String, val glyph: Glyph, val run: () -> Unit)

/** System actions the command bar can trigger. Implemented by the overlay service (needs a Context). */
interface SystemActions {
    fun torch(): Boolean
    fun wifiPanel()
    fun bluetoothSettings()
    fun internetPanel()
    fun launch(pkg: String)
    fun pinNote(text: String)
    fun openShelf()
    fun apps(): List<Pair<String, String>> // label, package
}

/**
 * Parses what the user types into the command bar. Pure logic, unit-tested.
 * "timer 10" = 10 min, "timer 90s", "t 5m", "focus 25", "note door code 4471", "torch", app names.
 */
object Commands {
    private val dur = Regex("""^(\d+(?:\.\d+)?)\s*(s|sec|secs|seconds?|m|min|mins|minutes?|h|hr|hrs|hours?)?$""")

    fun parseDuration(s: String, defaultUnitMs: Long = 60_000): Long? {
        val m = dur.find(s.trim().lowercase()) ?: return null
        val n = m.groupValues[1].toDouble()
        val unit = when (m.groupValues[2].firstOrNull()) {
            's' -> 1_000L; 'm' -> 60_000L; 'h' -> 3_600_000L; else -> defaultUnitMs
        }
        val ms = (n * unit).toLong()
        return if (ms in 1_000..86_400_000) ms else null
    }

    fun suggest(input: String, sys: SystemActions): List<Suggestion> {
        val q = input.trim()
        val lower = q.lowercase()
        val out = mutableListOf<Suggestion>()
        val word = lower.substringBefore(' ')
        val rest = q.substringAfter(' ', "").trim()

        if (word in setOf("timer", "t") && rest.isNotEmpty()) {
            parseDuration(rest)?.let { ms -> out += Suggestion("Start ${fmt(ms)} timer", "timer", Glyph.HOURGLASS) { Timers.startTimer(ms) } }
        }
        if (word == "focus" || word == "pomodoro") {
            val ms = parseDuration(rest.ifEmpty { "25" }) ?: 25 * 60_000L
            out += Suggestion("Focus for ${fmt(ms)}", "silences everything but calls", Glyph.FOCUS) { Timers.startFocus(ms) }
        }
        if (word == "note" && rest.isNotEmpty()) out += Suggestion("Pin “$rest”", "note", Glyph.PIN) { sys.pinNote(rest) }
        if (lower.isNotEmpty() && "torch".startsWith(lower) || lower == "flash" || lower == "flashlight") out += Suggestion("Torch", "toggle", Glyph.TORCH) { sys.torch() }
        if (lower.isNotEmpty() && "wifi".startsWith(lower.replace("-", ""))) out += Suggestion("Wi-Fi", "quick panel", Glyph.WIFI) { sys.wifiPanel() }
        if (lower.isNotEmpty() && "bluetooth".startsWith(lower)) out += Suggestion("Bluetooth", "settings", Glyph.BT) { sys.bluetoothSettings() }
        if (lower.isNotEmpty() && "shelf".startsWith(lower)) out += Suggestion("Shelf", "things you saved", Glyph.TRAY) { sys.openShelf() }
        // A bare number means a timer: "5" -> 5 min
        if (out.isEmpty()) parseDuration(q)?.let { ms -> out += Suggestion("Start ${fmt(ms)} timer", "timer", Glyph.HOURGLASS) { Timers.startTimer(ms) } }

        if (lower.length >= 2) {
            sys.apps().asSequence()
                .filter { (label, _) -> label.lowercase().split(' ').any { it.startsWith(lower) } || label.lowercase().startsWith(lower) }
                .take(4)
                .forEach { (label, pkg) -> out += Suggestion(label, "open app", Glyph.APP) { sys.launch(pkg) } }
        }
        return out.take(6)
    }

    fun fmt(ms: Long): String {
        val s = ms / 1000
        return when {
            s < 60 -> "${s}s"
            s % 60 == 0L && s < 3600 -> "${s / 60} min"
            s < 3600 -> "${s / 60}m ${s % 60}s"
            else -> "${s / 3600}h ${(s % 3600) / 60}m"
        }
    }
}
