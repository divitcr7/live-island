package app.liveisland.engine

import app.liveisland.core.IslandAction
import app.liveisland.core.IslandItem
import app.liveisland.core.Kind
import app.liveisland.island.Glyph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Timers and focus sessions that live inside the island itself (no clock app needed). */
object Timers {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val jobs = mutableMapOf<String, Job>()
    private var seq = 0

    /** Called when a focus session starts/stops so the overlay can toggle Do Not Disturb. */
    var onFocusChanged: ((Boolean) -> Unit)? = null

    fun startTimer(ms: Long, label: String = "Timer"): String {
        val id = "timer:${++seq}"
        schedule(id, Kind.TIMER, ms, label, Glyph.HOURGLASS)
        return id
    }

    fun startFocus(ms: Long) {
        val id = "focus"
        cancel(id)
        schedule(id, Kind.FOCUS, ms, "Focus", Glyph.FOCUS)
        onFocusChanged?.invoke(true)
    }

    private fun schedule(id: String, kind: Kind, ms: Long, label: String, glyph: Glyph) {
        val start = System.currentTimeMillis()
        val end = start + ms
        IslandEngine.post(
            IslandItem(
                id = id, kind = kind, title = label, glyph = glyph, endsAt = end, totalMs = ms,
                actions = listOf(
                    IslandAction("add", "1 min", Glyph.PLUS) { extend(id, 60_000) },
                    IslandAction("stop", "Stop", Glyph.STOP) { cancel(id) },
                ),
            )
        )
        jobs[id]?.cancel()
        jobs[id] = scope.launch {
            while (true) {
                val item = IslandEngine.items.value[id] ?: return@launch
                val left = (item.endsAt ?: return@launch) - System.currentTimeMillis()
                if (left <= 0) break
                delay(minOf(left, 1000))
            }
            finish(id, label, kind)
        }
    }

    fun extend(id: String, ms: Long) {
        val item = IslandEngine.items.value[id] ?: return
        IslandEngine.post(item.copy(endsAt = (item.endsAt ?: 0) + ms, totalMs = (item.totalMs ?: 0) + ms))
    }

    fun cancel(id: String) {
        jobs.remove(id)?.cancel()
        IslandEngine.remove(id)
        if (id == "focus") onFocusChanged?.invoke(false)
    }

    private fun finish(id: String, label: String, kind: Kind) {
        jobs.remove(id)
        IslandEngine.remove(id)
        if (kind == Kind.FOCUS) onFocusChanged?.invoke(false)
        val doneId = "$id:done"
        IslandEngine.post(
            IslandItem(
                id = doneId, kind = Kind.TIMER_DONE,
                title = if (kind == Kind.FOCUS) "Focus done" else "$label done",
                glyph = Glyph.CHECK, progress = 1f,
                expiresAt = System.currentTimeMillis() + 8_000,
                actions = listOf(IslandAction("ok", "OK", Glyph.CHECK, primary = true) { IslandEngine.remove(doneId) }),
            )
        )
        IslandEngine.haptic?.invoke(true)
    }
}
