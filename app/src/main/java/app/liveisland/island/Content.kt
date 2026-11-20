package app.liveisland.island

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.liveisland.core.Corners
import app.liveisland.core.IslandAction
import app.liveisland.core.IslandItem
import app.liveisland.core.Kind

/** Which side of the island the camera hole sits on. */
enum class LensSide { START, CENTER, END }

val TextHi = Color(0xFFEEECE7)
val TextLo = Color(0xFF8E8D93)
val TextFaint = Color(0xFF6E6D73)

const val COMPACT_H = 34f
const val LENS_GAP = 22f

internal val LocalStyle = compositionLocalOf { IslandStyle.preset(app.liveisland.core.ThemeId.SIGNAL) }
internal val LocalLens = compositionLocalOf { LensSide.CENTER }
internal val LocalAccent = compositionLocalOf { Color.White }

@Composable
fun ProvideIsland(style: IslandStyle, lens: LensSide, accent: Color, content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalStyle provides style, LocalLens provides lens, LocalAccent provides accent, content = content)

// ---------- text ----------

@Composable
fun T(
    text: String, size: TextUnit = 13.sp, color: Color = TextHi, weight: FontWeight = FontWeight.Normal,
    num: Boolean = false, caps: Boolean = false, modifier: Modifier = Modifier, maxLines: Int = 1,
) {
    val s = LocalStyle.current
    BasicText(
        if (caps) text.uppercase() else text,
        modifier,
        style = TextStyle(
            fontFamily = if (num || caps) s.fonts.num else s.fonts.text,
            fontSize = if (caps) 10.sp else size, color = color, fontWeight = weight,
            letterSpacing = if (caps) 0.6.sp else if (num) (-0.2).sp else 0.sp,
            fontFeatureSettings = if (num) "tnum" else null,
        ),
        maxLines = maxLines, overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun G(glyph: Glyph, size: Dp = 15.dp, tint: Color = TextHi, modifier: Modifier = Modifier) =
    Image(rememberVectorPainter(glyph.vector), null, modifier.size(size), colorFilter = ColorFilter.tint(tint))

// ---------- shared pieces ----------

@Composable
fun Art(item: IslandItem, size: Dp) {
    val r = if (LocalStyle.current.corners == Corners.TIGHT) size / 6 else size / 4.2f
    val m = Modifier.size(size).clip(RoundedCornerShape(r))
    val art = item.art
    if (art != null) Image(art, null, m, contentScale = ContentScale.Crop)
    else Box(m.background(Brush.linearGradient(listOf(Color(0xFFC79A7F), Color(0xFF6F5A86)))))
}

@Composable
fun Wave(playing: Boolean, bars: Int = 6, height: Dp = 14.dp) {
    val accent = LocalAccent.current
    val t = rememberInfiniteTransition(label = "wave")
    Row(Modifier.height(height), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(bars) { i ->
            val f by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(420 + (i * 137) % 380), RepeatMode.Reverse), label = "b$i")
            Box(Modifier.width(2.dp).fillMaxHeight(if (playing) f else 0.25f).clip(RoundedCornerShape(1.dp)).background(accent))
        }
    }
}

@Composable
fun Btn(a: IslandAction, big: Boolean = false, filled: Boolean = a.primary, tint: Color? = null) {
    val s = LocalStyle.current
    val shape: Shape = if (s.corners == Corners.TIGHT) RoundedCornerShape(10.dp) else CircleShape
    val size = if (big) 44.dp else 36.dp
    val bg = tint ?: if (filled) TextHi else Color.Transparent
    val fg = if (tint != null || filled) Color(0xFF0B0B0C) else TextHi
    Box(Modifier.size(size).clip(shape).background(bg).clickable { a.run() }, contentAlignment = Alignment.Center) {
        a.glyph?.let { G(it, if (big) 20.dp else 18.dp, fg) }
    }
}

@Composable
fun Pill(label: String, glyph: Glyph? = null, onClick: () -> Unit) {
    val s = LocalStyle.current
    Row(
        Modifier.clip(RoundedCornerShape(if (s.corners == Corners.TIGHT) 8.dp else 50.dp))
            .background(Color.White.copy(alpha = 0.09f)).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        glyph?.let { G(it, 13.dp) }
        T(label, 12.5.sp)
    }
}

// ---------- layout around the lens ----------

/** A compact row: [lead] on one side of the camera, [trail] on the other. */
@Composable
fun CompactRow(lead: @Composable RowScope.() -> Unit, trail: @Composable RowScope.() -> Unit) {
    val h = COMPACT_H.dp
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        when (LocalLens.current) {
            LensSide.CENTER -> {
                Row(Modifier.weight(1f).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp), content = lead)
                Spacer(Modifier.width(LENS_GAP.dp))
                Row(Modifier.weight(1f).padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.End), content = trail)
            }
            LensSide.START -> {
                Spacer(Modifier.width(h))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp), content = lead)
                Spacer(Modifier.weight(1f).width(10.dp))
                Row(Modifier.padding(end = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp), content = trail)
            }
            LensSide.END -> {
                Row(Modifier.padding(start = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp), content = lead)
                Spacer(Modifier.weight(1f).width(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp), content = trail)
                Spacer(Modifier.width(h))
            }
        }
    }
}

/** Header strip for expanded cards. Labels sit beside the lens, never under it. */
@Composable
fun CamBar(left: String, right: String = "") {
    val lens = LocalLens.current
    Row(
        Modifier.fillMaxWidth().height(COMPACT_H.dp).padding(
            start = if (lens == LensSide.START) COMPACT_H.dp else 20.dp,
            end = if (lens == LensSide.END) COMPACT_H.dp else 20.dp,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        T(left, color = TextLo, caps = true)
        Spacer(Modifier.weight(1f))
        T(right, color = TextLo, caps = true)
    }
}

@Composable
fun Card(left: String, right: String = "", content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        CamBar(left, right)
        Box(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) { content() }
    }
}

// ---------- time helpers ----------

fun clock(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

fun mediaPos(i: IslandItem, now: Long): Long =
    if (!i.playing || i.positionAt == 0L) i.positionMs
    else (i.positionMs + ((now - i.positionAt) * i.speed).toLong()).coerceIn(0, i.durationMs.coerceAtLeast(1))

/** Live rim progress for an item at [now]. */
fun liveProgress(i: IslandItem, now: Long): Float? = when {
    i.kind == Kind.MEDIA && i.durationMs > 0 -> mediaPos(i, now).toFloat() / i.durationMs
    i.endsAt != null && (i.totalMs ?: 0) > 0 -> ((i.endsAt - now).toFloat() / i.totalMs!!).coerceIn(0f, 1f)
    i.kind == Kind.CALL -> 1f
    else -> i.progress
}

fun liveValue(i: IslandItem, now: Long): String = when {
    i.endsAt != null -> clock(i.endsAt - now)
    i.startedAt != null -> clock(now - i.startedAt)
    else -> i.value
}
