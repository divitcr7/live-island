package app.liveisland.island

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.liveisland.R
import app.liveisland.core.Body
import app.liveisland.core.Corners
import app.liveisland.core.FontId
import app.liveisland.core.Kind
import app.liveisland.core.Motion
import app.liveisland.core.PaletteId
import app.liveisland.core.ProgressStyle
import app.liveisland.core.Settings
import app.liveisland.core.StyleOverrides
import app.liveisland.core.ThemeId

data class IslandPalette(
    val music: Color, val timer: Color, val nav: Color, val call: Color,
    val alert: Color, val neutral: Color, val live: Color, val msg: Color,
)

val Palettes = mapOf(
    PaletteId.SIGNAL to IslandPalette(
        music = Color(0xFFE9E7E2), timer = Color(0xFFE9E7E2), nav = Color(0xFFE9E7E2), call = Color(0xFFF2603C),
        alert = Color(0xFFF2603C), neutral = Color(0xFFE9E7E2), live = Color(0xFFE9E7E2), msg = Color(0xFFE9E7E2),
    ),
    PaletteId.MONO to IslandPalette(
        music = Color(0xFFE9E7E2), timer = Color(0xFFE9E7E2), nav = Color(0xFFE9E7E2), call = Color(0xFFE9E7E2),
        alert = Color(0xFFE9E7E2), neutral = Color(0xFFE9E7E2), live = Color(0xFFE9E7E2), msg = Color(0xFFE9E7E2),
    ),
    PaletteId.CODED to IslandPalette(
        music = Color(0xFFB4A3DB), timer = Color(0xFFDDA24E), nav = Color(0xFF86AECF), call = Color(0xFF73BF95),
        alert = Color(0xFFDE6B55), neutral = Color(0xFFE9E7E2), live = Color(0xFF86AECF), msg = Color(0xFFD99AB0),
    ),
    PaletteId.CLAY to IslandPalette(
        music = Color(0xFFC99B95), timer = Color(0xFFCDAE74), nav = Color(0xFF97AE9F), call = Color(0xFFA1B893),
        alert = Color(0xFFC97E68), neutral = Color(0xFFE2DCD1), live = Color(0xFF97AE9F), msg = Color(0xFFC99B95),
    ),
    PaletteId.ADAPTIVE to IslandPalette(
        music = Color(0xFFE48D6A), timer = Color(0xFFD9B45C), nav = Color(0xFF6FB59B), call = Color(0xFF7FBF7A),
        alert = Color(0xFFDE6B55), neutral = Color(0xFFE9E7E2), live = Color(0xFF6FB59B), msg = Color(0xFFE48D6A),
    ),
)

data class IslandFonts(val text: FontFamily, val num: FontFamily)

val Geist = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
)
val GeistMono = FontFamily(
    Font(R.font.geist_mono_regular, FontWeight.Normal),
    Font(R.font.geist_mono_medium, FontWeight.Medium),
    Font(R.font.geist_mono_medium, FontWeight.SemiBold),
)
val Figtree = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
)
val MartianMono = FontFamily(
    Font(R.font.martian_mono_regular, FontWeight.Normal),
    Font(R.font.martian_mono_medium, FontWeight.Medium),
    Font(R.font.martian_mono_semibold, FontWeight.SemiBold),
)

fun fontsFor(id: FontId) = when (id) {
    FontId.GEIST -> IslandFonts(Geist, GeistMono)
    FontId.ROUNDED -> IslandFonts(Figtree, Figtree)
    FontId.MONO -> IslandFonts(MartianMono, MartianMono)
}

/** The fully resolved look of the island. */
data class IslandStyle(
    val theme: ThemeId,
    val paletteId: PaletteId,
    val progress: ProgressStyle,
    val corners: Corners,
    val body: Body,
    val font: FontId,
    val motion: Motion,
) {
    val palette get() = Palettes.getValue(paletteId)
    val fonts get() = fontsFor(font)

    /** Corner radius for an island of [height]. Compact pills are always fully round unless Tight. */
    fun radius(height: Dp, expanded: Boolean): Dp {
        if (!expanded) return if (corners == Corners.TIGHT) 10.dp else height / 2
        val r = when (corners) { Corners.ROUND -> 34.dp; Corners.MEDIUM -> 24.dp; Corners.TIGHT -> 14.dp }
        return minOf(r, height / 2)
    }

    fun accent(kind: Kind, art: Color? = null): Color {
        if (paletteId == PaletteId.ADAPTIVE && art != null) return art
        val p = palette
        return when (kind) {
            Kind.MEDIA -> p.music
            Kind.TIMER, Kind.TIMER_DONE, Kind.FOCUS -> p.timer
            Kind.NAVIGATION, Kind.LIVE -> p.nav
            Kind.CALL, Kind.CHARGING, Kind.BLUETOOTH -> p.call
            Kind.OTP, Kind.PRIVACY -> p.alert
            Kind.MESSAGE -> p.msg
            else -> p.neutral
        }
    }

    fun <T> spring(): SpringSpec<T> = when (motion) {
        Motion.SNAPPY -> spring(dampingRatio = 1f, stiffness = 900f)
        Motion.SPRINGY -> spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
        Motion.SLOW -> spring(dampingRatio = 0.92f, stiffness = 110f)
    }

    companion object {
        fun preset(id: ThemeId) = when (id) {
            ThemeId.SIGNAL -> IslandStyle(id, PaletteId.SIGNAL, ProgressStyle.TICKS, Corners.ROUND, Body.BLACK, FontId.GEIST, Motion.SNAPPY)
            ThemeId.CODED -> IslandStyle(id, PaletteId.CODED, ProgressStyle.LINE, Corners.MEDIUM, Body.BLACK, FontId.GEIST, Motion.SPRINGY)
            ThemeId.INSTRUMENT -> IslandStyle(id, PaletteId.SIGNAL, ProgressStyle.TICKS, Corners.TIGHT, Body.BLACK, FontId.MONO, Motion.SNAPPY)
            ThemeId.CLAY -> IslandStyle(id, PaletteId.CLAY, ProgressStyle.FILL, Corners.ROUND, Body.TINTED, FontId.ROUNDED, Motion.SLOW)
        }

        fun resolve(id: ThemeId, o: StyleOverrides?): IslandStyle {
            val b = preset(id)
            if (o == null) return b
            return b.copy(
                paletteId = o.palette ?: b.paletteId, progress = o.progress ?: b.progress,
                corners = o.corners ?: b.corners, body = o.body ?: b.body,
                font = o.font ?: b.font, motion = o.motion ?: b.motion,
            )
        }

        fun of(s: Settings) = resolve(s.theme, s.overrides[s.theme])
    }
}
