package app.liveisland.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.liveisland.core.Settings
import app.liveisland.core.ThemeId
import app.liveisland.island.Glyph
import app.liveisland.island.IslandStyle
import app.liveisland.island.LensSide
import app.liveisland.ui.Demo
import app.liveisland.ui.Permissions
import app.liveisland.ui.PhoneTop
import app.liveisland.ui.design.Button
import app.liveisland.ui.design.Chip
import app.liveisland.ui.design.Entry
import app.liveisland.ui.design.Icon
import app.liveisland.ui.design.Label
import app.liveisland.ui.design.P
import app.liveisland.ui.design.Rule
import app.liveisland.ui.design.Status
import app.liveisland.ui.design.Switch
import app.liveisland.ui.design.Txt
import kotlinx.coroutines.delay

data class Access(val overlay: Boolean, val notifications: Boolean, val runtime: Boolean, val dnd: Boolean) {
    val ready get() = overlay && notifications
}

private val showcase = listOf(Demo.REST, Demo.MUSIC, Demo.STACKED, Demo.CALL, Demo.TIMER_OPEN, Demo.CHARGING, Demo.OTP, Demo.NAV)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    s: Settings, side: LensSide, access: Access, update: ((Settings) -> Settings) -> Unit,
    open: (Sub) -> Unit, dark: Boolean, refresh: () -> Unit,
) {
    val c = P.c
    val ctx = LocalContext.current
    var i by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(2600); i = (i + 1) % showcase.size } }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh() }

    Column(Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Label("open source · zero data")
                        Txt("Live Island", P.t.display)
                    }
                    Status(
                        when { !access.ready -> "Setup"; !s.enabled -> "Paused"; else -> "Running" },
                        access.ready && s.enabled,
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(c.surface).clickableNoRipple {
                            update { it.copy(appearance = if (dark) app.liveisland.core.Appearance.LIGHT else app.liveisland.core.Appearance.DARK) }
                        },
                        contentAlignment = Alignment.Center,
                    ) { Icon(if (dark) Glyph.SPARK else Glyph.MOON, 16.dp) }
                }
            }
            item {
                Box(Modifier.padding(top = 18.dp)) {
                    PhoneTop(IslandStyle.of(s), showcase[i], s.placement, side, s.rest, s.features, height = 230.dp)
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                    showcase.forEachIndexed { k, _ ->
                        Box(Modifier.padding(3.dp).size(if (k == i) 14.dp else 5.dp, 5.dp).clip(CircleShape).background(if (k == i) c.fg else c.line))
                    }
                }
            }

            if (!access.ready || !access.runtime) item {
                Column(Modifier.padding(top = 20.dp)) {
                    Label("Two switches and you're done")
                    Spacer(Modifier.height(4.dp))
                    Step(1, "Show the island", "Lets it draw over the status bar. It can't see your screen.", access.overlay) { Permissions.openOverlay(ctx) }
                    if (!access.overlay && Build.VERSION.SDK_INT >= 33) Row(Modifier.padding(start = 40.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Txt("Switch greyed out? ", P.t.small, c.muted)
                        Txt("Open App info → ⋮ → Allow restricted settings", P.t.small.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), c.fg,
                            modifier = Modifier.clickableNoRipple { Permissions.openAppInfo(ctx) })
                    }
                    Step(2, "Music, calls and notifications", "Read on this phone only. Nothing is stored or sent.", access.notifications) { Permissions.openNotifications(ctx) }
                    Step(3, "Calendar, calls and earbuds", "Optional. Next meeting, answering calls, earbud battery.", access.runtime) { ask.launch(Permissions.runtime) }
                }
            }

            item {
                Column(Modifier.padding(top = 22.dp)) {
                    Label("Theme")
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ThemeId.entries.forEach { t ->
                            Chip(t.label, s.theme == t, { update { it.copy(theme = t) } }, swatches = swatchesFor(t))
                        }
                    }
                }
            }
            item {
                Column(Modifier.padding(top = 14.dp)) {
                    Rule()
                    Entry("Island", if (s.enabled) "Always on screen" else "Hidden until you turn it back on", Glyph.DOT, onClick = { update { it.copy(enabled = !it.enabled) } }) {
                        Switch(s.enabled, { on -> update { it.copy(enabled = on) } })
                    }
                    Rule()
                    Entry("At rest", if (s.rest.mode == app.liveisland.core.RestMode.RING) "Ring around the camera" else s.rest.facts.joinToString(" · ") { it.label }, Glyph.CAL) { open(Sub.REST) }
                    Rule()
                    Entry("Privacy", "No internet permission. Nothing leaves this phone.", Glyph.SHIELD) { open(Sub.PRIVACY) }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun Step(n: Int, title: String, sub: String, done: Boolean, onClick: () -> Unit) {
    val c = P.c
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(if (done) c.fg else c.surface), contentAlignment = Alignment.Center) {
            if (done) Icon(Glyph.CHECK, 14.dp, c.inv) else Txt("$n", P.t.num, c.fg)
        }
        Column(Modifier.weight(1f)) {
            Txt(title, P.t.heading)
            Txt(sub, P.t.small, c.muted)
        }
        if (!done) Button("Turn on", onClick)
    }
}

fun swatchesFor(t: ThemeId) = IslandStyle.preset(t).palette.let { p -> listOf(p.music, p.timer, p.nav, p.call).distinct().take(4) }

private fun countOn(s: Settings): Int = with(s.features) {
    listOf(media, calls, otp, timers, navigation, liveUpdates, charging, privacy, bluetooth, notes, focus, shelf, commandBar, satellites).count { it }
}

fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.clickable(interactionSource = MutableInteractionSource(), indication = null, onClick = onClick)
