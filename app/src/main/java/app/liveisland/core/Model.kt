package app.liveisland.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import app.liveisland.island.Glyph

/** What kind of thing is on the island. Decides colour, layout and default priority. */
enum class Kind(val basePriority: Int) {
    CALL(100), OTP(95), TIMER_DONE(92), MESSAGE(80), CHARGING(78), BLUETOOTH(70), PRIVACY(65),
    NAVIGATION(60), TIMER(55), FOCUS(54), MEDIA(50), LIVE(48), PLUGIN(40), SHELF(30), NOTE(20),
}

data class IslandAction(
    val id: String,
    val label: String,
    val glyph: Glyph? = null,
    val primary: Boolean = false,
    val run: () -> Unit,
)

/**
 * One live activity. Sources create these; the engine orders them; the island draws them.
 * Times are wall-clock epoch millis so the UI can compute live text every frame.
 */
data class IslandItem(
    val id: String,
    val kind: Kind,
    val title: String = "",
    val subtitle: String = "",
    /** Short text for the compact pill's trailing side. */
    val value: String = "",
    val glyph: Glyph? = null,
    /** 0..1, drawn on the rim. */
    val progress: Float? = null,
    /** Count down to this moment (timers, deliveries, countdowns). */
    val endsAt: Long? = null,
    /** Count up from this moment (calls, stopwatches). */
    val startedAt: Long? = null,
    val totalMs: Long? = null,
    val art: ImageBitmap? = null,
    val artAccent: Color? = null,
    val packageName: String? = null,
    val actions: List<IslandAction> = emptyList(),
    /** Removed automatically after this moment. */
    val expiresAt: Long? = null,
    val priorityBoost: Int = 0,
    val playing: Boolean = true,
    val replies: List<String> = emptyList(),
    val onReply: ((String) -> Unit)? = null,
    val onOpen: (() -> Unit)? = null,
    /** Media only: position bookkeeping. */
    val positionMs: Long = 0,
    val positionAt: Long = 0,
    val durationMs: Long = 0,
    val speed: Float = 1f,
) {
    val priority get() = kind.basePriority + priorityBoost
}

/** Values shown on the resting island. */
data class FactValue(val glyph: Glyph, val value: String, val label: String)
