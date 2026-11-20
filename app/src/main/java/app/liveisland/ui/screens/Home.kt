package app.liveisland.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
            item { PowerCard(s, access, update) }
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

            if (access.ready && !access.runtime) item {
                Column(Modifier.padding(top = 14.dp)) {
                    Step(3, "Calendar, calls and earbuds", "Optional. Next meeting, answering calls, earbud battery.", false) { ask.launch(Permissions.runtime) }
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


/**
 * The first thing on Home. Not set up: one button that walks through the two Android
 * switches in order. Set up: a big on/off switch.
 */
@Composable
private fun PowerCard(s: Settings, access: Access, update: ((Settings) -> Settings) -> Unit) {
    val c = P.c
    val ctx = LocalContext.current
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
    val ready = access.ready
    val on = ready && s.enabled
    Column(
        Modifier.fillMaxWidth().padding(top = 16.dp).clip(shape).background(if (on || !ready) c.fg else c.surface)
            .then(if (!on && ready) Modifier.border(1.dp, c.line, shape) else Modifier)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val ink = if (on || !ready) c.inv else c.fg
        val soft = if (on || !ready) c.inv.copy(alpha = 0.65f) else c.muted
        if (!ready) {
            val step = if (!access.overlay) 1 else 2
            Label("Step $step of 2", color = soft)
            Txt("Live Island is off", P.t.title, ink)
            Txt(
                if (step == 1) "Tap Turn on, find Live Island in the list and switch it on. This lets it draw around your camera. It can't see your screen."
                else "One more: allow notification access so it can show music, calls, timers and codes. Nothing leaves your phone.",
                P.t.body, soft,
            )
            Box(
                Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp)).background(c.inv)
                    .clickableNoRipple { if (step == 1) Permissions.openOverlay(ctx) else Permissions.openNotifications(ctx) }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) { Txt(if (step == 1) "Turn on" else "Allow notifications", P.t.heading, c.fg) }
            if (step == 1 && Build.VERSION.SDK_INT >= 33) Txt(
                "Switch greyed out? Tap here, then ⋮ → Allow restricted settings, and come back.",
                P.t.small.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), soft,
                modifier = Modifier.clickableNoRipple { Permissions.openAppInfo(ctx) },
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Txt(if (on) "Island is on" else "Island is off", P.t.title, ink)
                    Txt(if (on) "Around your camera, on every app." else "Flip the switch to bring it back.", P.t.small, soft)
                }
                BigSwitch(on, onInk = on) { v -> update { it.copy(enabled = v) } }
            }
            Txt("Tip: add the Live Island tile to Quick Settings to switch it from anywhere.", P.t.small, soft)
        }
    }
}

@Composable
private fun BigSwitch(on: Boolean, onInk: Boolean, onChange: (Boolean) -> Unit) {
    val c = P.c
    val x by androidx.compose.animation.core.animateDpAsState(if (on) 30.dp else 3.dp, label = "k")
    val track = if (onInk) c.inv else c.line
    val knob = if (onInk) c.fg else c.raised
    Box(
        Modifier.size(64.dp, 37.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(11.dp)).background(track)
            .clickableNoRipple { onChange(!on) },
    ) {
        Box(Modifier.padding(start = x, top = 3.dp).size(31.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(9.dp)).background(knob))
    }
}
