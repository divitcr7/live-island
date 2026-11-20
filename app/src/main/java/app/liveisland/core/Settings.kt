package app.liveisland.core

import kotlinx.serialization.Serializable

/** Everything the user can choose. Stored on-device only, as JSON in DataStore. */
@Serializable
data class Settings(
    val enabled: Boolean = true,
    val theme: ThemeId = ThemeId.SIGNAL,
    val overrides: Map<ThemeId, StyleOverrides> = emptyMap(),
    val placement: Placement = Placement(),
    val rest: RestSettings = RestSettings(),
    val features: Features = Features(),
    val hideInApps: Set<String> = emptySet(),
    val blockedApps: Set<String> = emptySet(),
    val pinnedNote: String? = null,
    val countdowns: List<Countdown> = emptyList(),
    val onboarded: Boolean = false,
    val appearance: Appearance = Appearance.SYSTEM,
)

@Serializable
enum class ThemeId(val label: String, val blurb: String) {
    SIGNAL("Signal", "Black and off-white. Orange only for things that need you now."),
    CODED("Coded", "Each activity gets a muted colour of its own, traced by a thin line."),
    INSTRUMENT("Instrument", "Monospace, tight corners and tick marks. Reads like a gauge."),
    CLAY("Clay", "Soft earth tones, rounded type, slow movement. Fills instead of outlines."),
}

@Serializable
data class StyleOverrides(
    val palette: PaletteId? = null,
    val progress: ProgressStyle? = null,
    val corners: Corners? = null,
    val body: Body? = null,
    val font: FontId? = null,
    val motion: Motion? = null,
) {
    val isEmpty get() = this == StyleOverrides()
}

@Serializable enum class PaletteId(val label: String) { SIGNAL("Signal"), MONO("Mono"), CODED("Coded"), CLAY("Clay"), ADAPTIVE("Adaptive") }
@Serializable enum class ProgressStyle(val label: String) { LINE("Edge line"), TICKS("Ticks"), UNDERLINE("Underline"), FILL("Fill"), NONE("None") }
@Serializable enum class Corners(val label: String) { ROUND("Round"), MEDIUM("Medium"), TIGHT("Tight") }
@Serializable enum class Body(val label: String) { BLACK("Black"), OUTLINE("Black + outline"), TINTED("Tinted") }
@Serializable enum class FontId(val label: String) { GEIST("Geist"), ROUNDED("Figtree"), MONO("Martian Mono") }
@Serializable enum class Motion(val label: String) { SNAPPY("Snappy"), SPRINGY("Springy"), SLOW("Slow") }

@Serializable
enum class Anchor(val label: String) { AUTO("Auto"), LEFT("Left"), CENTER("Centre"), RIGHT("Right") }

@Serializable
data class Placement(
    val anchor: Anchor = Anchor.AUTO,
    /** Nudge in dp on top of the detected camera position. */
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    /** 0.8 .. 1.3 */
    val scale: Float = 1f,
    val autoHideFullscreen: Boolean = true,
)

@Serializable
enum class RestMode(val label: String) { FACTS("Facts"), RING("Ring only") }

@Serializable
enum class Fact(val label: String) {
    NEXT_EVENT("Next event"), BATTERY("Battery"), DATE("Date"), NET_SPEED("Network speed"),
    COUNTDOWN("Countdown"), NOTE("Pinned note"),
}

@Serializable
data class RestSettings(
    val mode: RestMode = RestMode.FACTS,
    val facts: List<Fact> = listOf(Fact.NEXT_EVENT, Fact.BATTERY, Fact.DATE),
    val rotateSeconds: Int = 4,
)

@Serializable enum class PeekMode(val label: String) { MESSAGES("Messages"), ALL("All notifications"), OFF("Off") }
@Serializable enum class Haptics(val label: String) { OFF("Off"), LIGHT("Light"), STRONG("Strong") }

@Serializable
data class Features(
    val media: Boolean = true,
    val calls: Boolean = true,
    val peek: PeekMode = PeekMode.MESSAGES,
    val otp: Boolean = true,
    val timers: Boolean = true,
    val navigation: Boolean = true,
    val liveUpdates: Boolean = true,
    val charging: Boolean = true,
    val chargingStats: Boolean = false,
    val privacy: Boolean = true,
    val bluetooth: Boolean = true,
    val netSpeed: Boolean = false,
    val notes: Boolean = true,
    val countdowns: Boolean = true,
    val focus: Boolean = true,
    val shelf: Boolean = true,
    val commandBar: Boolean = true,
    val satellites: Boolean = true,
    val plugins: Boolean = false,
    val haptics: Haptics = Haptics.LIGHT,
)

@Serializable enum class Appearance(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

@Serializable
data class Countdown(val title: String, val atEpochMs: Long)
