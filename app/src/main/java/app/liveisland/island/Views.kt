package app.liveisland.island

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.liveisland.core.Corners
import app.liveisland.core.Fact
import app.liveisland.core.FactValue
import app.liveisland.core.IslandItem
import app.liveisland.core.Kind

data class Dims(val w: Dp, val h: Dp, val expanded: Boolean)

private val compactH = COMPACT_H.dp

/** Kinds that arrive big (they interrupt) rather than as a compact pill. */
fun naturallyBig(i: IslandItem) = when (i.kind) {
    Kind.CALL -> i.startedAt == null
    Kind.MESSAGE, Kind.OTP, Kind.CHARGING, Kind.TIMER_DONE, Kind.BLUETOOTH -> true
    else -> false
}

fun hasExpanded(i: IslandItem) = when (i.kind) {
    Kind.MEDIA, Kind.TIMER, Kind.FOCUS, Kind.NAVIGATION, Kind.LIVE, Kind.CALL, Kind.PRIVACY, Kind.NOTE, Kind.PLUGIN -> true
    else -> false
}

fun compactDims(i: IslandItem): Dims = when (i.kind) {
    Kind.CALL -> if (i.startedAt == null) Dims(320.dp, 108.dp, true) else Dims(200.dp, compactH, false)
    Kind.MESSAGE -> Dims(320.dp, 150.dp, true)
    Kind.OTP -> Dims(300.dp, 108.dp, true)
    Kind.CHARGING -> Dims(236.dp, 56.dp, true)
    Kind.TIMER_DONE -> Dims(236.dp, 56.dp, true)
    Kind.BLUETOOTH -> Dims(260.dp, 56.dp, true)
    Kind.MEDIA -> Dims(208.dp, compactH, false)
    Kind.TIMER, Kind.FOCUS -> Dims(186.dp, compactH, false)
    Kind.NAVIGATION -> Dims(212.dp, compactH, false)
    Kind.PRIVACY -> Dims(196.dp, compactH, false)
    Kind.LIVE, Kind.PLUGIN, Kind.NOTE -> Dims(228.dp, compactH, false)
    Kind.SHELF -> Dims(150.dp, compactH, false)
}

fun expandedDims(i: IslandItem): Dims = when (i.kind) {
    Kind.MEDIA -> Dims(320.dp, 186.dp, true)
    Kind.TIMER, Kind.FOCUS -> Dims(280.dp, 142.dp, true)
    Kind.NAVIGATION -> Dims(320.dp, 136.dp, true)
    Kind.LIVE, Kind.PLUGIN -> Dims(320.dp, 116.dp, true)
    Kind.CALL -> Dims(300.dp, 108.dp, true)
    Kind.PRIVACY -> Dims(280.dp, 92.dp, true)
    Kind.NOTE -> Dims(300.dp, 104.dp, true)
    else -> compactDims(i)
}

// ---------- rest ----------

@Composable
fun RestView(fact: FactValue?) {
    if (fact == null) return
    CompactRow(
        lead = { G(fact.glyph, 13.dp); T(fact.value, num = true, weight = FontWeight.Medium) },
        trail = { T(fact.label, color = TextLo) },
    )
}

fun restWidth(ringOnly: Boolean): Dp = if (ringOnly) 40.dp else 180.dp

fun pickFact(facts: Map<Fact, FactValue>, order: List<Fact>, now: Long, everySec: Int): FactValue? {
    val avail = order.mapNotNull { facts[it] }
    if (avail.isEmpty()) return null
    return avail[((now / 1000) / everySec.coerceAtLeast(2) % avail.size).toInt()]
}

// ---------- compact ----------

@Composable
fun CompactView(i: IslandItem, now: Long) {
    val accent = LocalAccent.current
    when (i.kind) {
        Kind.MEDIA -> CompactRow(
            lead = { Art(i, 20.dp); T(clock(mediaPos(i, now)), color = TextLo, num = true) },
            trail = { Wave(i.playing) },
        )
        Kind.TIMER, Kind.FOCUS -> CompactRow(
            lead = { G(i.glyph ?: Glyph.HOURGLASS, 14.dp, accent); T(i.title) },
            trail = { T(liveValue(i, now), color = accent, num = true, weight = FontWeight.Medium) },
        )
        Kind.NAVIGATION -> CompactRow(
            lead = { G(Glyph.TURN, 15.dp, accent); T(i.value.ifEmpty { i.title }, num = true, weight = FontWeight.Medium) },
            trail = { T(i.subtitle, color = TextLo, num = true) },
        )
        Kind.CALL -> CompactRow(
            lead = { G(Glyph.PHONE, 13.dp, accent); T(liveValue(i, now), num = true, weight = FontWeight.Medium) },
            trail = { Wave(true, 5) },
        )
        Kind.PRIVACY -> CompactRow(
            lead = {
                Box(Modifier.size(7.dp).clip(CircleShape).background(accent))
                G(i.glyph ?: Glyph.CAMERA, 14.dp, accent)
            },
            trail = { T(i.subtitle.ifEmpty { i.title }, color = TextLo) },
        )
        Kind.LIVE, Kind.PLUGIN -> CompactRow(
            lead = { G(i.glyph ?: Glyph.BOX, 14.dp, accent); T(i.title, modifier = Modifier.weight(1f, false)) },
            trail = { T(liveValue(i, now).ifEmpty { i.progress?.let { "${(it * 100).toInt()}%" } ?: "" }, num = true, weight = FontWeight.Medium) },
        )
        Kind.NOTE -> CompactRow(
            lead = { G(Glyph.PIN, 13.dp, accent); T(i.title, modifier = Modifier.weight(1f, false)) },
            trail = {},
        )
        Kind.SHELF -> CompactRow(lead = { G(Glyph.TRAY, 14.dp) }, trail = { T(i.value, num = true) })
        else -> BigView(i, now)
    }
}

// ---------- naturally big ----------

@Composable
fun BigView(i: IslandItem, now: Long) {
    val accent = LocalAccent.current
    when (i.kind) {
        Kind.CALL -> Card("Incoming call", i.subtitle) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(i.title)
                Column(Modifier.weight(1f)) { T(i.title, 14.sp, weight = FontWeight.SemiBold); T("Swipe up to hide", 12.sp, TextLo) }
                i.actions.getOrNull(0)?.let { Btn(it, big = true, tint = LocalStyle.current.palette.alert) }
                i.actions.getOrNull(1)?.let { Btn(it, big = true, tint = accent) }
            }
        }
        Kind.MESSAGE -> Card(i.subtitle.ifEmpty { "Message" }, "now") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Avatar(i.title, 34.dp)
                    Column(Modifier.weight(1f)) {
                        T(i.title, 13.5.sp, weight = FontWeight.SemiBold)
                        T(i.value, 13.sp, Color(0xFFD5D3CE), maxLines = 2)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    val send = i.onReply
                    if (send != null) i.replies.take(2).forEach { r -> Pill(r) { send(r) } }
                    Spacer(Modifier.weight(1f))
                    i.onOpen?.let { open -> Pill("Open") { open() } }
                }
            }
        }
        Kind.OTP -> Card("Code", i.subtitle) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                T(i.value.chunked(3).joinToString(" "), 30.sp, accent, num = true, weight = FontWeight.Medium, modifier = Modifier.weight(1f))
                i.actions.firstOrNull()?.let { Pill(it.label, it.glyph) { it.run() } }
            }
        }
        Kind.CHARGING, Kind.TIMER_DONE, Kind.BLUETOOTH -> Row(
            Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            G(i.glyph ?: Glyph.BOLT, 16.dp, accent)
            Spacer(Modifier.width(6.dp))
            T(i.value.ifEmpty { i.title }, 21.sp, accent, num = true, weight = FontWeight.Medium)
            Spacer(Modifier.weight(1f).width(LENS_GAP.dp))
            Column(horizontalAlignment = Alignment.End) {
                T(if (i.value.isEmpty()) "" else i.title, 11.5.sp, TextLo)
                T(i.subtitle, 12.5.sp, num = true)
            }
        }
        else -> CompactView(i, now)
    }
}

@Composable
fun Avatar(name: String, size: Dp = 42.dp) {
    val initials = name.split(' ', '-').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }
    Box(Modifier.size(size).clip(CircleShape).background(Color(0xFFD8D2C4)), contentAlignment = Alignment.Center) {
        T(initials.ifEmpty { "•" }, 13.sp, Color(0xFF1B1B1D), weight = FontWeight.SemiBold)
    }
}

// ---------- expanded ----------

@Composable
fun ExpandedView(i: IslandItem, now: Long) {
    val accent = LocalAccent.current
    when (i.kind) {
        Kind.MEDIA -> Card("Now playing", i.subtitle.takeIf { it.length < 18 } ?: "") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Art(i, 48.dp)
                    Column(Modifier.weight(1f)) { T(i.title, 14.sp, weight = FontWeight.SemiBold); T(i.value, 12.5.sp, TextLo) }
                    i.actions.firstOrNull { it.id == "like" }?.let { Btn(it) }
                }
                val pos = mediaPos(i, now)
                Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.14f))) {
                    Box(Modifier.fillMaxWidth(if (i.durationMs > 0) (pos.toFloat() / i.durationMs).coerceIn(0f, 1f) else 0f).height(3.dp).background(TextHi))
                }
                Row { T(clock(pos), 10.5.sp, TextLo, num = true); Spacer(Modifier.weight(1f)); T(clock(i.durationMs), 10.5.sp, TextLo, num = true) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    i.actions.filter { it.id != "like" }.forEach { Btn(it, big = it.primary) }
                }
            }
        }
        Kind.TIMER, Kind.FOCUS -> Card(i.title, if (i.kind == Kind.FOCUS) "do not disturb" else "") {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                T(liveValue(i, now), 42.sp, accent, num = true, weight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { i.actions.forEach { a -> Pill(a.label, a.glyph) { a.run() } } }
            }
        }
        Kind.NAVIGATION -> Card("Navigate", i.subtitle) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val r = if (LocalStyle.current.corners == Corners.TIGHT) 8.dp else 14.dp
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(r)).background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    val art = i.art
                    if (art != null) androidx.compose.foundation.Image(art, null, Modifier.size(32.dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(accent))
                    else G(Glyph.TURN, 26.dp, accent)
                }
                Column(Modifier.weight(1f)) {
                    T(i.value.ifEmpty { i.title }, 22.sp, num = true, weight = FontWeight.Medium)
                    T(if (i.value.isEmpty()) "" else i.title, 12.5.sp, TextLo, maxLines = 2)
                }
            }
        }
        Kind.LIVE, Kind.PLUGIN -> Card(i.subtitle.ifEmpty { "Live" }, liveValue(i, now)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                T(i.title, 15.sp, weight = FontWeight.SemiBold, maxLines = 2)
                val p = liveProgress(i, now)
                if (p != null) Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.12f))) {
                    Box(Modifier.fillMaxWidth(p).height(4.dp).background(accent))
                } else T(i.value, 12.5.sp, TextLo)
            }
        }
        Kind.CALL -> Card("On a call", liveValue(i, now)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(i.title, 38.dp)
                T(i.title, 14.sp, weight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                i.actions.forEach { Btn(it, tint = if (it.id == "end") LocalStyle.current.palette.alert else null) }
            }
        }
        Kind.PRIVACY -> Card("Sensors in use") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                G(i.glyph ?: Glyph.CAMERA, 18.dp, accent)
                Column { T(i.title, 14.sp, weight = FontWeight.SemiBold); T(i.subtitle.ifEmpty { "Unknown app" }, 12.5.sp, TextLo) }
            }
        }
        Kind.NOTE -> Card("Pinned") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                T(i.title, 15.sp, maxLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { i.actions.forEach { a -> Pill(a.label, a.glyph) { a.run() } } }
            }
        }
        else -> BigView(i, now)
    }
}

/** Small round bubble beside the main pill for a secondary activity. */
@Composable
fun SatelliteView(i: IslandItem, now: Long, onClick: (() -> Unit)?) {
    val accent = LocalAccent.current
    val p = liveProgress(i, now)
    val tight = LocalStyle.current.corners == Corners.TIGHT
    Box(
        Modifier.size(COMPACT_H.dp).clip(if (tight) RoundedCornerShape(10.dp) else CircleShape)
            .background(Color.Black).then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (p != null) androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
            val sw = 2.dp.toPx()
            drawArc(Color.White.copy(alpha = 0.14f), 0f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
            drawArc(accent, -90f, 360f * p, false, style = androidx.compose.ui.graphics.drawscope.Stroke(sw, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        if (i.kind == Kind.MEDIA && i.art != null) Box(Modifier.size(16.dp).clip(CircleShape)) { Art(i, 16.dp) }
        else G(i.glyph ?: glyphFor(i.kind), 12.dp, accent)
    }
}

fun glyphFor(k: Kind) = when (k) {
    Kind.MEDIA -> Glyph.PLAY; Kind.TIMER -> Glyph.HOURGLASS; Kind.FOCUS -> Glyph.FOCUS; Kind.NAVIGATION -> Glyph.TURN
    Kind.CALL -> Glyph.PHONE; Kind.LIVE -> Glyph.BOX; Kind.PRIVACY -> Glyph.CAMERA; Kind.NOTE -> Glyph.PIN
    Kind.BLUETOOTH -> Glyph.BT; Kind.CHARGING -> Glyph.BOLT; Kind.OTP -> Glyph.KEY; Kind.MESSAGE -> Glyph.BELL
    else -> Glyph.DOT
}

@Suppress("unused")
private fun Modifier.tap(f: (() -> Unit)?) = if (f == null) this else clickable { f() }
