package app.liveisland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.liveisland.core.Fact
import app.liveisland.core.FactValue
import app.liveisland.core.Features
import app.liveisland.core.IslandAction
import app.liveisland.core.IslandItem
import app.liveisland.core.Kind
import app.liveisland.core.Placement
import app.liveisland.core.RestSettings
import app.liveisland.engine.IslandUi
import app.liveisland.engine.Mode
import app.liveisland.island.Geist
import app.liveisland.island.Glyph
import app.liveisland.island.Island
import app.liveisland.island.IslandStyle
import app.liveisland.island.LensSide
import app.liveisland.ui.design.P
import kotlinx.coroutines.delay

/** States you can preview without them actually happening. */
enum class Demo(val label: String) {
    REST("At rest"), MUSIC("Music"), MUSIC_OPEN("Music, open"), STACKED("Stacked"), TIMER("Timer"), TIMER_OPEN("Timer, open"),
    NAV("Navigation"), CALL("Incoming call"), ON_CALL("On a call"), MESSAGE("Message"), OTP("OTP code"), CHARGING("Charging"),
    LIVE("Delivery"), PRIVACY("Camera in use"), BLUETOOTH("Earbuds"), NOTE("Pinned note"), FOCUS("Focus"), SHRUNK("Shrunk"),
}

object DemoData {
    private val noop: () -> Unit = {}
    fun facts(now: Long) = mapOf(
        Fact.NEXT_EVENT to FactValue(Glyph.CAL, "23m", "Standup"),
        Fact.BATTERY to FactValue(Glyph.BATTERY, "86%", "Battery"),
        Fact.DATE to FactValue(Glyph.CAL, "20", "Fri"),
        Fact.NET_SPEED to FactValue(Glyph.DOWN, "4.2 MB/s", "↑ 120 KB/s"),
        Fact.COUNTDOWN to FactValue(Glyph.FLAG, "3d", "Goa trip"),
        Fact.NOTE to FactValue(Glyph.PIN, "Note", "door 4471"),
    )

    private fun media(t0: Long, now: Long) = IslandItem(
        "media", Kind.MEDIA, title = "Midnight Static", value = "Lowtide Club", subtitle = "Buds",
        playing = true, positionMs = 72_000, positionAt = t0, durationMs = 208_000,
        actions = listOf(
            IslandAction("prev", "Previous", Glyph.PREV, run = noop),
            IslandAction("play", "Pause", Glyph.PAUSE, primary = true, run = noop),
            IslandAction("next", "Next", Glyph.NEXT, run = noop),
        ),
    )
    private fun timer(t0: Long) = IslandItem(
        "timer", Kind.TIMER, title = "Steep", glyph = Glyph.HOURGLASS, endsAt = t0 + 168_000, totalMs = 180_000,
        actions = listOf(IslandAction("add", "1 min", Glyph.PLUS, run = noop), IslandAction("stop", "Stop", Glyph.STOP, run = noop)),
    )
    private fun nav() = IslandItem("nav", Kind.NAVIGATION, title = "Right onto Bridge St", value = "200 m", subtitle = "14:32", progress = 0.62f)

    fun build(d: Demo, t0: Long, now: Long): Pair<Map<String, IslandItem>, IslandUi> {
        val auto = IslandUi()
        val list: List<IslandItem> = when (d) {
            Demo.REST, Demo.SHRUNK -> emptyList()
            Demo.MUSIC, Demo.MUSIC_OPEN -> listOf(media(t0, now))
            Demo.STACKED -> listOf(media(t0, now), timer(t0), nav().copy(priorityBoost = -20))
            Demo.TIMER, Demo.TIMER_OPEN -> listOf(timer(t0))
            Demo.NAV -> listOf(nav())
            Demo.CALL -> listOf(IslandItem("call", Kind.CALL, title = "Maya Rao", subtitle = "mobile", actions = listOf(
                IslandAction("decline", "Decline", Glyph.PHONE, run = noop), IslandAction("answer", "Answer", Glyph.PHONE, primary = true, run = noop))))
            Demo.ON_CALL -> listOf(IslandItem("call", Kind.CALL, title = "Maya Rao", startedAt = t0 - 63_000))
            Demo.MESSAGE -> listOf(IslandItem("peek", Kind.MESSAGE, title = "Jules", value = "Are we still on for 7? I booked the table by the window.", subtitle = "Messages",
                replies = listOf("👍", "On my way"), onReply = { }, onOpen = noop))
            Demo.OTP -> listOf(IslandItem("otp", Kind.OTP, value = "482913", subtitle = "HDFC Bank", actions = listOf(IslandAction("copy", "Copy", Glyph.COPY, primary = true, run = noop))))
            Demo.CHARGING -> listOf(IslandItem("charging", Kind.CHARGING, title = "Charging", value = "86%", subtitle = "full in 38m", glyph = Glyph.BOLT, progress = 0.86f))
            Demo.LIVE -> listOf(IslandItem("live", Kind.LIVE, title = "Order on the way", value = "Rider is 1.2 km away", subtitle = "Swiggy", endsAt = t0 + 6 * 60_000, totalMs = 20 * 60_000, glyph = Glyph.BOX))
            Demo.PRIVACY -> listOf(IslandItem("privacy", Kind.PRIVACY, title = "Camera + mic", subtitle = "Meet", glyph = Glyph.CAMERA))
            Demo.BLUETOOTH -> listOf(IslandItem("bt", Kind.BLUETOOTH, title = "Connected", value = "Buds Pro", subtitle = "L 82% · R 79%", glyph = Glyph.BT, progress = 0.8f))
            Demo.NOTE -> listOf(IslandItem("note", Kind.NOTE, title = "Parking B2-14, pillar 31", actions = listOf(IslandAction("unpin", "Unpin", Glyph.CLOSE, run = noop))))
            Demo.FOCUS -> listOf(IslandItem("focus", Kind.FOCUS, title = "Focus", glyph = Glyph.FOCUS, endsAt = t0 + 18 * 60_000, totalMs = 25 * 60_000,
                actions = listOf(IslandAction("stop", "Stop", Glyph.STOP, run = noop))))
        }
        val ui = when (d) {
            Demo.MUSIC_OPEN, Demo.TIMER_OPEN -> auto.copy(mode = Mode.EXPANDED)
            Demo.SHRUNK -> auto.copy(mode = Mode.SHRUNK)
            else -> auto
        }
        return list.associateBy { it.id } to ui
    }
}

/** Ticks once a second inside previews. */
@Composable
fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000 - now % 1000); now = System.currentTimeMillis() } }
    return now
}

private const val SCREEN_W = 392f
private const val BEZEL = 8f

/**
 * The top of a phone, drawn to scale: bezel, status bar, an app behind, and the real island
 * composable sitting on the camera. Every preview in the app is this.
 */
@Composable
fun PhoneTop(
    style: IslandStyle,
    demo: Demo,
    placement: Placement,
    side: LensSide,
    rest: RestSettings,
    features: Features,
    modifier: Modifier = Modifier,
    height: Dp = 250.dp,
    onDrag: ((dxDp: Float, dyDp: Float) -> Unit)? = null,
) {
    val now = rememberNow()
    val t0 = remember(demo) { System.currentTimeMillis() }
    val (items, ui) = remember(demo, now / 1000) { DemoData.build(demo, t0, now) }
    val facts = remember(now / 1000) { DemoData.facts(now) }
    val drag by rememberUpdatedState(onDrag)

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val s = maxWidth.value / (SCREEN_W + BEZEL * 2)
        val base = LocalDensity.current
        val boxH = height
        Box(Modifier.fillMaxWidth().height(boxH)) {
            CompositionLocalProvider(LocalDensity provides Density(base.density * s, 1f)) {
                val virtualH = (boxH.value / s).dp
                Box(
                    Modifier.width((SCREEN_W + BEZEL * 2).dp).height(virtualH)
                        .clip(RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp)).background(Color(0xFF1C1C1E))
                        .padding(start = BEZEL.dp, end = BEZEL.dp, top = BEZEL.dp)
                ) {
                    val dark = P.c.dark
                    val app = if (dark) Color(0xFF0E0E10) else Color.White
                    val fg = if (dark) Color(0xFFECEBE7) else Color(0xFF151517)
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 42.dp, topEnd = 42.dp)).background(app)) {
                        FakeApp(fg, if (dark) Color(0xFF26262A) else Color(0xFFEDEDEF))
                        val lensX = when (side) { LensSide.START -> 32f; LensSide.END -> SCREEN_W - 32f; LensSide.CENTER -> SCREEN_W / 2 }
                        val showStatus = !(side == LensSide.CENTER) || true
                        if (showStatus) StatusBar(fg, side)
                        val gestureMod = if (drag != null) Modifier.pointerInput(Unit) {
                            detectDragGestures { c, d -> c.consume(); drag?.invoke(d.x / density, d.y / density) }
                        } else Modifier
                        LensPlacer(lensX + placement.offsetX, 22f + placement.offsetY, side, placement.scale, gestureMod) {
                            Island(
                                style = style, items = items, ui = ui, facts = facts, rest = rest, features = features,
                                lens = side, now = now, scale = placement.scale, host = null, drawLens = true, onModeChange = {},
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Places the island so its lens point lands on (lensX, lensY), whatever its current width. */
@Composable
private fun LensPlacer(lensX: Float, lensY: Float, side: LensSide, scale: Float, modifier: Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier.fillMaxSize()) { ms, c ->
        val p = ms.first().measure(androidx.compose.ui.unit.Constraints())
        val h = 34f * scale * density
        val lx = lensX * density
        val x = when (side) {
            LensSide.CENTER -> lx - p.width / 2f
            LensSide.START -> lx - h / 2f
            LensSide.END -> lx + h / 2f - p.width
        }
        val y = lensY * density - h / 2f
        layout(c.maxWidth, c.maxHeight) { p.place(x.toInt(), y.toInt()) }
    }
}

@Composable
private fun StatusBar(fg: Color, side: LensSide) {
    Row(
        Modifier.fillMaxWidth().height(44.dp).padding(start = 30.dp, end = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (side != LensSide.START) BasicText("14:07", style = TextStyle(fontFamily = Geist, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = fg))
        Spacer(Modifier.weight(1f))
        if (side != LensSide.END) Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(4) { i -> Box(Modifier.width(3.dp).height((4 + i * 2.5f).dp).clip(RoundedCornerShape(1.dp)).background(fg.copy(alpha = if (i == 3) 0.35f else 1f))) }
            Spacer(Modifier.width(2.dp))
            Box(Modifier.size(22.dp, 11.dp).clip(RoundedCornerShape(3.dp)).background(fg.copy(alpha = 0.35f)).padding(1.5.dp)) {
                Box(Modifier.fillMaxSize(0.86f).clip(RoundedCornerShape(2.dp)).background(fg))
            }
        }
        else BasicText("14:07", style = TextStyle(fontFamily = Geist, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = fg))
    }
}

@Composable
private fun FakeApp(fg: Color, soft: Color) {
    Column(Modifier.fillMaxSize().padding(top = 66.dp, start = 22.dp, end = 22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BasicText("Messages", style = TextStyle(fontFamily = Geist, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = fg, letterSpacing = (-0.6).sp))
        Box(Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(12.dp)).background(soft))
        listOf("Jules" to "Are we still on for 7?", "Maya Rao" to "Sent the deck, have a look", "Dad" to "Call me when free", "Rohan" to "haha yes, same time").forEach { (n, m) ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(soft))
                Column {
                    BasicText(n, style = TextStyle(fontFamily = Geist, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = fg))
                    BasicText(m, style = TextStyle(fontFamily = Geist, fontSize = 13.5.sp, color = fg.copy(alpha = 0.55f)))
                }
            }
        }
    }
}
