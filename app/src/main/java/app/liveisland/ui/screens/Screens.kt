package app.liveisland.ui.screens

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.liveisland.core.Anchor
import app.liveisland.core.Body
import app.liveisland.core.Corners
import app.liveisland.core.Countdown
import app.liveisland.core.Fact
import app.liveisland.core.Features
import app.liveisland.core.FontId
import app.liveisland.core.Haptics
import app.liveisland.core.Motion
import app.liveisland.core.PaletteId
import app.liveisland.core.PeekMode
import app.liveisland.core.Placement
import app.liveisland.core.ProgressStyle
import app.liveisland.core.RestMode
import app.liveisland.core.Settings
import app.liveisland.core.StyleOverrides
import app.liveisland.core.ThemeId
import app.liveisland.island.Glyph
import app.liveisland.island.IslandStyle
import app.liveisland.island.LensSide
import app.liveisland.island.Palettes
import app.liveisland.sources.PluginReceiver
import app.liveisland.ui.Demo
import app.liveisland.ui.Permissions
import app.liveisland.ui.PhoneTop
import app.liveisland.ui.design.Button
import app.liveisland.ui.design.Choice
import app.liveisland.ui.design.Field
import app.liveisland.ui.design.Icon
import app.liveisland.ui.design.Label
import app.liveisland.ui.design.P
import app.liveisland.ui.design.Rule
import app.liveisland.ui.design.Section
import app.liveisland.ui.design.Segmented
import app.liveisland.ui.design.Slider
import app.liveisland.ui.design.Switch
import app.liveisland.ui.design.ToggleEntry
import app.liveisland.ui.design.Txt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Bottom tabs. Every main area is one tap away. */
enum class Tab(val label: String, val glyph: Glyph) {
    HOME("Home", Glyph.ISLAND), LOOK("Look", Glyph.SPARK), ACTIVITIES("Activities", Glyph.SLIDERS), POSITION("Position", Glyph.MOVE), MORE("More", Glyph.MORE),
}

/** Pages that open on top of a tab, one level deep. */
enum class Sub(val title: String) { REST("At rest"), APPS("Apps"), AUTOMATION("Automation"), PRIVACY("Privacy") }

typealias Update = ((Settings) -> Settings) -> Unit

// =====================================================================
// Look
// =====================================================================

private val lookDemos = listOf(Demo.MUSIC, Demo.MUSIC_OPEN, Demo.STACKED, Demo.TIMER, Demo.TIMER_OPEN, Demo.CALL, Demo.CHARGING, Demo.OTP, Demo.REST)

@Composable
fun LookScreen(s: Settings, side: LensSide, update: Update, back: (() -> Unit)? = null) {
    var demo by remember { mutableStateOf(Demo.MUSIC) }
    val style = IslandStyle.of(s)
    val o = s.overrides[s.theme] ?: StyleOverrides()
    fun set(f: (StyleOverrides) -> StyleOverrides) = update { st -> st.copy(overrides = st.overrides + (st.theme to f(st.overrides[st.theme] ?: StyleOverrides()))) }

    Page("Look", back, preview = {
        Column {
            PhoneTop(style, demo, s.placement, side, s.rest, s.features, height = 215.dp)
            DemoPicker(lookDemos, demo) { demo = it }
        }
    }) {
        item {
            Section("Theme") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    ThemeId.entries.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { t -> ThemeCard(t, s.theme == t, s.overrides[t]?.isEmpty == false, Modifier.weight(1f)) { update { it.copy(theme = t) } } }
                        }
                    }
                }
            }
        }
        item {
            Section("Customise ${s.theme.label}") {
                Choice("Colour") {
                    Segmented(PaletteId.entries, style.paletteId, { it.label }, { v -> set { it.copy(palette = v) } }) { id ->
                        Palettes.getValue(id).let { listOf(it.music, it.timer, it.nav, it.call).distinct().take(4) }
                    }
                }
                Rule()
                Choice("Progress", "How timers, songs and trips show on the edge.") { Segmented(ProgressStyle.entries, style.progress, { it.label }, { v -> set { it.copy(progress = v) } }) }
                Rule()
                Choice("Corners when open") { Segmented(Corners.entries, style.corners, { it.label }, { v -> set { it.copy(corners = v) } }) }
                Rule()
                Choice("Body") { Segmented(Body.entries, style.body, { it.label }, { v -> set { it.copy(body = v) } }) }
                Rule()
                Choice("Typeface") { Segmented(FontId.entries, style.font, { it.label }, { v -> set { it.copy(font = v) } }) }
                Rule()
                Choice("Motion") { Segmented(Motion.entries, style.motion, { it.label }, { v -> set { it.copy(motion = v) } }) }
                if (!o.isEmpty) {
                    Spacer(Modifier.height(8.dp))
                    Button("Reset ${s.theme.label} to default", { update { it.copy(overrides = it.overrides - it.theme) } }, filled = false)
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(t: ThemeId, selected: Boolean, customised: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = P.c
    val st = IslandStyle.preset(t)
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(c.surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) c.fg else c.line, RoundedCornerShape(14.dp))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF0E0E10)), contentAlignment = Alignment.Center) {
            val now = app.liveisland.ui.rememberNow()
            val t0 = remember { System.currentTimeMillis() }
            val (items, ui) = remember(now / 1000) { app.liveisland.ui.DemoData.build(Demo.TIMER, t0, now) }
            app.liveisland.island.Island(st, items, ui, emptyMap(), app.liveisland.core.RestSettings(), Features(satellites = false), LensSide.CENTER, now, scale = 0.82f, drawLens = true, onModeChange = {})
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(t.label, P.t.heading, modifier = Modifier.weight(1f))
            if (customised) Label("edited")
        }
        Txt(t.blurb, P.t.small, c.muted, maxLines = 3)
    }
}

// =====================================================================
// Position & size
// =====================================================================

@Composable
fun PositionScreen(s: Settings, side: LensSide, detected: String, update: Update, back: (() -> Unit)? = null) {
    val p = s.placement
    fun set(f: (Placement) -> Placement) = update { it.copy(placement = f(it.placement)) }
    Page("Position", back, preview = {
        Column {
            PhoneTop(IslandStyle.of(s), Demo.MUSIC, p, side, s.rest, s.features, height = 170.dp) { dx, dy ->
                set { it.copy(offsetX = (it.offsetX + dx).coerceIn(-120f, 120f), offsetY = (it.offsetY + dy).coerceIn(-20f, 60f)) }
            }
            Txt("Drag the island to move it. Your real island moves too.", P.t.small, P.c.muted, modifier = Modifier.padding(vertical = 10.dp))
        }
    }) {
        item {
            Choice("Camera", "Detected: $detected. Change it if your camera sits somewhere else.") {
                Segmented(Anchor.entries, p.anchor, { if (it == Anchor.AUTO) "Auto" else it.label }, { v -> set { it.copy(anchor = v) } })
            }
            Rule()
            Choice("Size  ${(p.scale * 100).toInt()}%") {
                Slider(p.scale, 0.8f..1.3f, { v -> set { it.copy(scale = v) } })
            }
            Rule()
            Choice("Fine-tune", "x ${"%.0f".format(p.offsetX)} dp · y ${"%.0f".format(p.offsetY)} dp") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Nudge(Glyph.BACK) { set { it.copy(offsetX = it.offsetX - 1) } }
                    Nudge(Glyph.UP) { set { it.copy(offsetY = it.offsetY - 1) } }
                    Nudge(Glyph.DOWN) { set { it.copy(offsetY = it.offsetY + 1) } }
                    Nudge(Glyph.CHEVRON) { set { it.copy(offsetX = it.offsetX + 1) } }
                    Spacer(Modifier.width(6.dp))
                    Button("Reset", { set { it.copy(offsetX = 0f, offsetY = 0f, scale = 1f) } }, filled = false)
                }
            }
            Rule()
            ToggleEntry("Hide in full-screen apps", "Videos and games get the whole screen.", p.autoHideFullscreen) { v -> set { it.copy(autoHideFullscreen = v) } }
        }
    }
}

@Composable
private fun Nudge(g: Glyph, onClick: () -> Unit) {
    val c = P.c
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(12.dp))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(g, 18.dp) }
}

// =====================================================================
// At rest
// =====================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RestScreen(s: Settings, side: LensSide, update: Update, back: () -> Unit) {
    val ctx = LocalContext.current
    val r = s.rest
    var note by remember { mutableStateOf(s.pinnedNote ?: "") }
    var cdTitle by remember { mutableStateOf("") }
    Page("At rest", back, preview = {
        Column {
            PhoneTop(IslandStyle.of(s), Demo.REST, s.placement, side, r, s.features, height = 170.dp)
            Txt("When nothing is happening, this is what stays on screen.", P.t.small, P.c.muted, modifier = Modifier.padding(vertical = 10.dp))
        }
    }) {
        item {
            Choice("Show") { Segmented(RestMode.entries, r.mode, { it.label }, { v -> update { it.copy(rest = it.rest.copy(mode = v)) } }) }
            if (r.mode == RestMode.FACTS) {
                Rule()
                Choice("Facts", "Tap in the order you want them. They take turns.") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Fact.entries.forEach { f ->
                            val idx = r.facts.indexOf(f)
                            app.liveisland.ui.design.Chip(if (idx >= 0) "${idx + 1}  ${f.label}" else f.label, idx >= 0, {
                                update { st ->
                                    val list = st.rest.facts
                                    val next = if (f in list) list - f else list + f
                                    var feats = st.features
                                    if (f == Fact.NET_SPEED && f !in list) feats = feats.copy(netSpeed = true)
                                    st.copy(rest = st.rest.copy(facts = next.ifEmpty { listOf(Fact.BATTERY) }), features = feats)
                                }
                            })
                        }
                    }
                }
                Rule()
                Choice("Switch every") {
                    Segmented(listOf(3, 4, 6, 10), r.rotateSeconds, { "${it}s" }, { v -> update { it.copy(rest = it.rest.copy(rotateSeconds = v)) } })
                }
            }
        }
        item {
            Section("Pinned note") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Field(note, { note = it }, "Door code, parking spot, anything", glyph = Glyph.PIN)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(if (s.pinnedNote == null) "Pin to island" else "Update", { update { it.copy(pinnedNote = note.trim().ifEmpty { null }) } })
                        if (s.pinnedNote != null) Button("Unpin", { note = ""; update { it.copy(pinnedNote = null) } }, filled = false)
                    }
                    Txt("Tip: hold the island and type “note …” to pin from anywhere.", P.t.small, P.c.muted)
                }
            }
        }
        item {
            Section("Countdowns") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    val fmt = remember { SimpleDateFormat("EEE d MMM", Locale.getDefault()) }
                    s.countdowns.sortedBy { it.atEpochMs }.forEach { cd ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Icon(Glyph.FLAG, 16.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) { Txt(cd.title, P.t.heading); Txt(fmt.format(Date(cd.atEpochMs)), P.t.small, P.c.muted) }
                            Box(Modifier.size(36.dp).clickable { update { it.copy(countdowns = it.countdowns - cd) } }, contentAlignment = Alignment.Center) { Icon(Glyph.CLOSE, 16.dp, P.c.muted) }
                        }
                    }
                    Field(cdTitle, { cdTitle = it }, "Flight, exam, trip…", glyph = Glyph.FLAG)
                    Button("Pick a date", {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(ctx, { _, y, m, d ->
                            val at = Calendar.getInstance().apply { set(y, m, d, 9, 0, 0) }.timeInMillis
                            val title = cdTitle.trim().ifEmpty { "Countdown" }
                            update { st -> st.copy(countdowns = st.countdowns + Countdown(title, at), rest = st.rest.copy(facts = if (Fact.COUNTDOWN in st.rest.facts) st.rest.facts else st.rest.facts + Fact.COUNTDOWN)) }
                            cdTitle = ""
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    }, filled = false, glyph = Glyph.CAL)
                }
            }
        }
    }
}

// =====================================================================
// Activities
// =====================================================================

private class Feat(val title: String, val sub: String, val glyph: Glyph, val demo: Demo?, val get: (Features) -> Boolean, val set: (Features, Boolean) -> Features)

private val feats = listOf(
    Feat("Music", "Any player. Art, waveform, seek, skip.", Glyph.PLAY, Demo.MUSIC, { it.media }, { f, v -> f.copy(media = v) }),
    Feat("Calls", "Answer or decline from the island.", Glyph.PHONE, Demo.CALL, { it.calls }, { f, v -> f.copy(calls = v) }),
    Feat("One-tap OTP", "Login codes from SMS with a Copy button.", Glyph.KEY, Demo.OTP, { it.otp }, { f, v -> f.copy(otp = v) }),
    Feat("Timers & stopwatch", "From your clock app or the island itself.", Glyph.HOURGLASS, Demo.TIMER, { it.timers }, { f, v -> f.copy(timers = v) }),
    Feat("Navigation", "Next turn and arrival from Maps, Waze and others.", Glyph.TURN, Demo.NAV, { it.navigation }, { f, v -> f.copy(navigation = v) }),
    Feat("Deliveries, rides, downloads", "Live progress from any app.", Glyph.BOX, Demo.LIVE, { it.liveUpdates }, { f, v -> f.copy(liveUpdates = v) }),
    Feat("Charging", "Percentage and time to full.", Glyph.BOLT, Demo.CHARGING, { it.charging }, { f, v -> f.copy(charging = v) }),
    Feat("Charging stats", "Real watts and battery temperature.", Glyph.SPEED, Demo.CHARGING, { it.chargingStats }, { f, v -> f.copy(chargingStats = v) }),
    Feat("Camera & mic watch", "Which app is using them, by name.", Glyph.CAMERA, Demo.PRIVACY, { it.privacy }, { f, v -> f.copy(privacy = v) }),
    Feat("Earbuds & watch", "Battery when they connect.", Glyph.BT, Demo.BLUETOOTH, { it.bluetooth }, { f, v -> f.copy(bluetooth = v) }),
    Feat("Pinned notes", "Keep a reminder on the island.", Glyph.PIN, Demo.NOTE, { it.notes }, { f, v -> f.copy(notes = v) }),
    Feat("Focus sessions", "Timer that silences everything but calls.", Glyph.FOCUS, Demo.FOCUS, { it.focus }, { f, v -> f.copy(focus = v) }),
    Feat("Satellites", "Run several activities at once.", Glyph.DOT, Demo.STACKED, { it.satellites }, { f, v -> f.copy(satellites = v) }),
    Feat("Command bar", "Hold the island to type: timer 10, torch, apps.", Glyph.SEARCH, null, { it.commandBar }, { f, v -> f.copy(commandBar = v) }),
    Feat("Shelf", "Share or drag things onto the island to hold them.", Glyph.TRAY, null, { it.shelf }, { f, v -> f.copy(shelf = v) }),
    Feat("Countdowns", "Days until a trip, an exam, a flight.", Glyph.FLAG, null, { it.countdowns }, { f, v -> f.copy(countdowns = v) }),
)

@Composable
fun ActivitiesScreen(s: Settings, side: LensSide, dnd: Boolean, update: Update, back: (() -> Unit)? = null) {
    val ctx = LocalContext.current
    var demo by remember { mutableStateOf(Demo.MUSIC) }
    Page("Activities", back, preview = {
        Column {
            PhoneTop(IslandStyle.of(s), demo, s.placement, side, s.rest, s.features, height = 200.dp)
            Txt("Tap a row to preview it. The switch turns it on or off.", P.t.small, P.c.muted, modifier = Modifier.padding(vertical = 10.dp))
        }
    }) {
        items(feats) { f ->
            val on = f.get(s.features)
            Column {
                app.liveisland.ui.design.Entry(
                    f.title, f.sub, f.glyph,
                    onClick = { f.demo?.let { demo = it } ?: update { st -> st.copy(features = f.set(st.features, !on)) } },
                ) { Switch(on, { v -> update { st -> st.copy(features = f.set(st.features, v)) }; if (v) f.demo?.let { demo = it } }) }
                if (f.demo == Demo.FOCUS && on && !dnd) Row(Modifier.padding(start = 50.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button("Allow Do Not Disturb", { Permissions.openDnd(ctx) }, filled = false)
                }
                Rule()
            }
        }
        item {
            Choice("Notification peeks", "Short pop-ups with inline reply.") {
                Segmented(PeekMode.entries, s.features.peek, { it.label }, { v -> update { st -> st.copy(features = st.features.copy(peek = v)) }; demo = Demo.MESSAGE })
            }
            Rule()
            Choice("Haptics") { Segmented(Haptics.entries, s.features.haptics, { it.label }, { v -> update { st -> st.copy(features = st.features.copy(haptics = v)) } }) }
        }
    }
}

// =====================================================================
// Apps
// =====================================================================

@Composable
fun AppsScreen(s: Settings, update: Update, back: () -> Unit) {
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    val apps by produceState(emptyList<Pair<String, String>>()) {
        value = withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                .map { it.loadLabel(pm).toString() to it.activityInfo.packageName }
                .filter { it.second != ctx.packageName }.distinctBy { it.second }.sortedBy { it.first.lowercase() }
        }
    }
    Page("Apps", back) {
        item {
            Txt("Choose which apps can use the island, and where the island should get out of the way.", P.t.small, P.c.muted, modifier = Modifier.padding(vertical = 8.dp))
            Field(q, { q = it }, "Search apps", glyph = Glyph.SEARCH)
            Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp)) {
                Spacer(Modifier.weight(1f))
                Label("hide here", Modifier.width(76.dp))
                Label("on island", Modifier.width(60.dp))
            }
        }
        items(apps.filter { q.isBlank() || it.first.contains(q, true) }, key = { it.second }) { (label, pkg) ->
            Column {
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Txt(label, P.t.body, modifier = Modifier.weight(1f), maxLines = 1)
                    Box(Modifier.width(76.dp)) {
                        Switch(pkg in s.hideInApps, { v -> update { it.copy(hideInApps = if (v) it.hideInApps + pkg else it.hideInApps - pkg) } })
                    }
                    Switch(pkg !in s.blockedApps, { v -> update { it.copy(blockedApps = if (v) it.blockedApps - pkg else it.blockedApps + pkg) } })
                }
                Rule()
            }
        }
    }
}

// =====================================================================
// Automation
// =====================================================================

@Composable
fun AutomationScreen(s: Settings, update: Update, back: () -> Unit) {
    val ctx = LocalContext.current
    fun copy(t: String) = ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("intent", t))
    val show = "am broadcast -a ${PluginReceiver.ACTION_SHOW} --es id build --es title \"Build #42\" --es text \"Running tests\" --ei progress 60 --ei seconds 120"
    val dismiss = "am broadcast -a ${PluginReceiver.ACTION_DISMISS} --es id build"
    Page("Automation", back) {
        item {
            ToggleEntry("Let other apps post", "Tasker, Automate, MacroDroid or your own scripts.", s.features.plugins, Glyph.PLUG) { v ->
                update { it.copy(features = it.features.copy(plugins = v)) }
            }
            Rule()
            Section("Show something") { Code(show) { copy(show) } }
            Section("Remove it") { Code(dismiss) { copy(dismiss) } }
            Section("Extras") {
                Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "id" to "which item to create or update", "title" to "main text", "text" to "second line",
                        "source" to "small label, e.g. Home Assistant", "progress" to "0–100, drawn on the rim",
                        "countdown" to "seconds to count down", "seconds" to "remove after this long",
                    ).forEach { (k, v) ->
                        Row { Txt(k, P.t.num, modifier = Modifier.width(96.dp)); Txt(v, P.t.small, P.c.muted) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Code(text: String, onCopy: () -> Unit) {
    val c = P.c
    Column(
        Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(12.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Txt(text, P.t.num.copy(fontSize = P.t.small.fontSize), c.fg)
        Button("Copy", onCopy, filled = false, glyph = Glyph.COPY)
    }
}

// =====================================================================
// Privacy
// =====================================================================

@Composable
fun PrivacyScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    val internet = remember { Permissions.hasInternet(ctx) }
    Page("Privacy", back) {
        item {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Txt("Nothing leaves your phone.", P.t.title)
                Txt(
                    "Live Island has no account, no analytics, no ads and no crash reporting. It doesn't even ask Android for internet access, so it physically can't send anything anywhere.",
                    P.t.body, P.c.muted,
                )
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(if (internet) Glyph.CLOSE else Glyph.CHECK, 18.dp, if (internet) P.c.warn else P.c.ok)
                        Txt(if (internet) "This build requests internet access" else "Checked just now: no internet permission", P.t.heading)
                    }
                    Txt("Read live from this app's own manifest.", P.t.small, P.c.muted)
                }
            }
        }
        item {
            Section("What each permission is for") {
                Column(Modifier.padding(top = 6.dp)) {
                    listOf(
                        "Accessibility" to "Drawing the island over the status bar, and knowing which app is open so it can hide where you asked. It doesn't read what's on screen.",
                        "Notification access" to "Music, calls, timers, navigation, deliveries and codes. Read on the phone and dropped when they disappear.",
                        "Calendar" to "Your next event at rest. Read only.",
                        "Phone calls" to "Answer and decline buttons.",
                        "Nearby devices" to "Earbud and watch names and battery.",
                        "Do Not Disturb" to "Focus sessions, only if you allow it.",
                    ).forEach { (k, v) ->
                        Column(Modifier.padding(vertical = 10.dp)) { Txt(k, P.t.heading); Txt(v, P.t.small, P.c.muted) }
                        Rule()
                    }
                }
            }
        }
        item {
            Section("Open source") {
                Txt("Every line is public. If something here isn't true, the code will show it.", P.t.small, P.c.muted, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

// =====================================================================
// More
// =====================================================================

@Composable
fun MoreScreen(s: Settings, update: Update, open: (Sub) -> Unit) {
    Page("More", null) {
        item {
            Choice("App appearance") {
                Segmented(app.liveisland.core.Appearance.entries, s.appearance, { it.label }, { v -> update { it.copy(appearance = v) } })
            }
            Rule()
            app.liveisland.ui.design.Entry("At rest", if (s.rest.mode == RestMode.RING) "Ring around the camera" else s.rest.facts.joinToString(" · ") { it.label }, Glyph.CAL) { open(Sub.REST) }
            Rule()
            app.liveisland.ui.design.Entry("Apps", "Where it shows, where it hides", Glyph.APP) { open(Sub.APPS) }
            Rule()
            app.liveisland.ui.design.Entry("Automation", if (s.features.plugins) "On · Tasker and scripts can post" else "Off", Glyph.PLUG) { open(Sub.AUTOMATION) }
            Rule()
            app.liveisland.ui.design.Entry("Privacy", "No internet permission", Glyph.SHIELD) { open(Sub.PRIVACY) }
            Rule()
            Txt("Live Island 0.1.0 · open source", P.t.small, P.c.dim, modifier = Modifier.padding(top = 18.dp))
        }
    }
}
