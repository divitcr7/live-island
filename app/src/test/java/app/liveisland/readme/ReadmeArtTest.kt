package app.liveisland.readme

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityOptionsCompat
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.liveisland.core.Anchor
import app.liveisland.core.Appearance
import app.liveisland.core.Settings
import app.liveisland.core.ThemeId
import app.liveisland.island.Geist
import app.liveisland.island.GeistMono
import app.liveisland.island.IslandStyle
import app.liveisland.island.LensSide
import app.liveisland.ui.Demo
import app.liveisland.ui.PhoneTop
import app.liveisland.ui.design.PaperTheme
import app.liveisland.ui.screens.Access
import app.liveisland.ui.screens.ActivitiesScreen
import app.liveisland.ui.screens.HomeScreen
import app.liveisland.ui.screens.LookScreen
import app.liveisland.ui.screens.PositionScreen
import app.liveisland.ui.screens.Tab
import app.liveisland.ui.screens.TabBar
import com.android.resources.Density as Dpi
import org.junit.Rule
import org.junit.Test

private val Ground = Color(0xFF0A0A0C)
private val Hi = Color(0xFFEDEBE6)
private val Lo = Color(0xFF8C8A93)
private val Line = Color(0xFF232328)

/** Paparazzi caps images at 1000 px, so big art is rendered as 500x500 dp tiles at 2x and stitched afterwards. */
private const val TILE = 500
private fun canvas(@Suppress("UNUSED_PARAMETER") wDp: Int, @Suppress("UNUSED_PARAMETER") hDp: Int) = DeviceConfig.PIXEL_6.copy(
    screenWidth = TILE * 2, screenHeight = TILE * 2, density = Dpi.XHIGH, xdpi = 320, ydpi = 320, softButtons = false,
)

private fun Paparazzi.tiles(name: String, wDp: Int, hDp: Int, content: @Composable () -> Unit) {
    val cols = (wDp + TILE - 1) / TILE
    val rows = (hDp + TILE - 1) / TILE
    for (r in 0 until rows) for (c in 0 until cols) snapshot("${name}_${wDp}x${hDp}_r${r}c${c}") {
        Box(Modifier.fillMaxSize().background(Ground)) {
            Box(
                Modifier.wrapContentSize(Alignment.TopStart, unbounded = true)
                    .offset(x = (-c * TILE).dp, y = (-r * TILE).dp)
                    .requiredSize(wDp.dp, hDp.dp)
            ) { content() }
        }
    }
}

private val noRegistry = object : ActivityResultRegistryOwner {
    override val activityResultRegistry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) = Unit
    }
}

@Composable private fun H(text: String, size: Int, color: Color = Hi, weight: FontWeight = FontWeight.SemiBold, mono: Boolean = false, spacing: Float = -0.03f) =
    BasicText(text, style = TextStyle(fontFamily = if (mono) GeistMono else Geist, fontSize = size.sp, fontWeight = weight, color = color, letterSpacing = (size * spacing).sp))

@Composable private fun Eyebrow(text: String) = H(text.uppercase(), 13, Lo, FontWeight.Normal, mono = true, spacing = 0.12f)

@Composable
private fun Phone(theme: ThemeId, demo: Demo, side: LensSide = LensSide.CENTER, height: Dp = 150.dp, modifier: Modifier = Modifier) {
    val s = Settings(theme = theme)
    PhoneTop(IslandStyle.of(s), demo, s.placement, side, s.rest, s.features, modifier, height)
}

/** Renders a real app screen inside a phone outline at a fixed scale. */
@Composable
private fun Screen(w: Dp, h: Dp, dark: Boolean, content: @Composable () -> Unit) {
    val base = LocalDensity.current
    val scale = w.value / 392f
    Box(Modifier.size(w, h).clip(RoundedCornerShape(30.dp)).border(6.dp, Color(0xFF1C1C1E), RoundedCornerShape(30.dp)).padding(6.dp).clip(RoundedCornerShape(24.dp))) {
        CompositionLocalProvider(LocalDensity provides Density(base.density * scale, 1f), LocalActivityResultRegistryOwner provides noRegistry) {
            PaperTheme(dark) { content() }
        }
    }
}

class ReadmeArtTest {
    @get:Rule val hero = Paparazzi(deviceConfig = canvas(1280, 640), maxPercentDifference = 100.0)

    @Test fun hero() = hero.tiles("hero", 1280, 640) {
        PaperTheme(true) {
            Row(Modifier.fillMaxSize().background(Ground).padding(56.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(470.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Eyebrow("open source · android · zero data")
                    H("Live Island", 76, spacing = -0.045f)
                    H("The island that stays.\nAround your camera, on every app,\nrunning everything at once.", 22, Lo, FontWeight.Normal, spacing = -0.01f)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                        listOf("Signal", "Coded", "Instrument", "Clay").forEach { t ->
                            Box(Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, Line, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 7.dp)) { H(t, 14, Hi, FontWeight.Medium, spacing = 0f) }
                        }
                    }
                }
                Spacer(Modifier.width(40.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Phone(ThemeId.SIGNAL, Demo.MUSIC_OPEN, height = 238.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Phone(ThemeId.CODED, Demo.STACKED, height = 120.dp, modifier = Modifier.weight(1f))
                        Phone(ThemeId.CLAY, Demo.TIMER, LensSide.END, height = 120.dp, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Phone(ThemeId.INSTRUMENT, Demo.CHARGING, LensSide.START, height = 120.dp, modifier = Modifier.weight(1f))
                        Phone(ThemeId.SIGNAL, Demo.OTP, height = 120.dp, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

class ReadmeThemesTest {
    @get:Rule val p = Paparazzi(deviceConfig = canvas(1280, 760), maxPercentDifference = 100.0)

    @Test fun themes() = p.tiles("themes", 1280, 760) {
        PaperTheme(true) {
            Column(Modifier.fillMaxSize().background(Ground).padding(48.dp), verticalArrangement = Arrangement.spacedBy(26.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Eyebrow("four themes · every detail customisable")
                    H("Pick a look. Then make it yours.", 40)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    ThemeId.entries.forEach { t ->
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            H(t.label, 24)
                            H(t.blurb, 14, Lo, FontWeight.Normal, spacing = 0f)
                            Phone(t, Demo.MUSIC, height = 92.dp)
                            Phone(t, Demo.TIMER_OPEN, height = 190.dp)
                            Phone(t, Demo.CALL, height = 150.dp)
                        }
                    }
                }
            }
        }
    }
}

class ReadmeStatesTest {
    @get:Rule val p = Paparazzi(deviceConfig = canvas(1280, 900), maxPercentDifference = 100.0)

    private val grid = listOf(
        Demo.REST to "Always there", Demo.MUSIC to "Music", Demo.STACKED to "Three at once",
        Demo.ON_CALL to "On a call", Demo.NAV to "Navigation", Demo.LIVE to "Delivery",
        Demo.OTP to "One-tap OTP", Demo.CHARGING to "Charging", Demo.PRIVACY to "Camera watch",
        Demo.MESSAGE to "Quick reply", Demo.BLUETOOTH to "Earbuds", Demo.NOTE to "Pinned note",
    )

    @Test fun states() = p.tiles("states", 1280, 900) {
        PaperTheme(true) {
            Column(Modifier.fillMaxSize().background(Ground).padding(48.dp), verticalArrangement = Arrangement.spacedBy(26.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Eyebrow("17 activities · one island")
                    H("Everything Android islands forgot.", 40)
                }
                grid.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        row.forEach { (d, label) ->
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Phone(ThemeId.CODED, d, height = if (d == Demo.MESSAGE || d == Demo.OTP) 150.dp else 112.dp)
                                H(label, 16, Hi, FontWeight.Medium, spacing = 0f)
                            }
                        }
                    }
                }
            }
        }
    }
}

class ReadmeCamerasTest {
    @get:Rule val p = Paparazzi(deviceConfig = canvas(1280, 420), maxPercentDifference = 100.0)

    @Test fun cameras() = p.tiles("cameras", 1280, 420) {
        PaperTheme(true) {
            Column(Modifier.fillMaxSize().background(Ground).padding(48.dp), verticalArrangement = Arrangement.spacedBy(26.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Eyebrow("reads your display cutout")
                    H("Left, centre or right. It grows from your camera.", 40)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    listOf(LensSide.START to "Left camera", LensSide.CENTER to "Centre camera", LensSide.END to "Right camera").forEach { (side, label) ->
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Phone(ThemeId.INSTRUMENT, Demo.STACKED, side, height = 120.dp)
                            H(label, 16, Hi, FontWeight.Medium, spacing = 0f)
                        }
                    }
                }
            }
        }
    }
}

class ReadmeAppTest {
    @get:Rule val p = Paparazzi(deviceConfig = canvas(1280, 860), maxPercentDifference = 100.0)

    @Test fun app() = p.tiles("app", 1280, 860) {
        val s = Settings(theme = ThemeId.SIGNAL, appearance = Appearance.DARK)
        val ok = Access(true, true, true, true)
        Column(Modifier.fillMaxSize().background(Ground).padding(48.dp), verticalArrangement = Arrangement.spacedBy(26.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Eyebrow("the app · light and dark")
                H("Two switches to set up. Live previews for everything.", 40)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                val w = 272.dp; val h = 590.dp
                Screen(w, h, false) { Column { Box(Modifier.weight(1f)) { HomeScreen(s, LensSide.CENTER, Access(false, false, false, false), {}, {}, false, {}) }; TabBar(Tab.HOME) {} } }
                Screen(w, h, true) { Column { Box(Modifier.weight(1f)) { LookScreen(s.copy(theme = ThemeId.INSTRUMENT), LensSide.CENTER, {}) }; TabBar(Tab.LOOK) {} } }
                Screen(w, h, true) { Column { Box(Modifier.weight(1f)) { ActivitiesScreen(s.copy(theme = ThemeId.CODED), LensSide.CENTER, true, {}) }; TabBar(Tab.ACTIVITIES) {} } }
                Screen(w, h, false) { Column { Box(Modifier.weight(1f)) { PositionScreen(s.copy(theme = ThemeId.CLAY, placement = s.placement.copy(anchor = Anchor.LEFT)), LensSide.START, "left", {}) }; TabBar(Tab.POSITION) {} } }
            }
        }
    }
}
