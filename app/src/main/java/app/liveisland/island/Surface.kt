package app.liveisland.island

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.liveisland.core.Body
import app.liveisland.core.ProgressStyle

private val Ink = Color(0xFF000000)

/**
 * The island's body: shape, fill, outline and the progress rim.
 * The same composable draws the real overlay and every preview in the app.
 */
@Composable
fun IslandSurface(
    style: IslandStyle,
    width: Dp,
    height: Dp,
    radius: Dp,
    accent: Color,
    progress: Float?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    val body = if (style.body == Body.TINTED) lerp(Color(0xFF060607), accent, 0.15f) else Ink
    val rim = remember { RimCache() }
    Box(
        modifier
            .size(width, height)
            .clip(shape)
            .background(body)
            .drawWithContent {
                val p = progress?.coerceIn(0f, 1f)
                if (style.progress == ProgressStyle.FILL && p != null) {
                    drawRect(accent.copy(alpha = 0.22f), size = Size(size.width * p, size.height))
                }
                drawContent()
                if (style.body == Body.OUTLINE) {
                    val s = 1.dp.toPx()
                    drawRoundRect(
                        Color.White.copy(alpha = 0.16f), topLeft = Offset(s / 2, s / 2),
                        size = Size(size.width - s, size.height - s),
                        cornerRadius = CornerRadius(radius.toPx() - s / 2), style = Stroke(s),
                    )
                }
                if (p != null) when (style.progress) {
                    ProgressStyle.LINE -> rim.line(this, radius.toPx(), p, accent)
                    ProgressStyle.TICKS -> rim.ticks(this, radius.toPx(), p, accent)
                    ProgressStyle.UNDERLINE -> {
                        val inset = 16.dp.toPx(); val th = 2.dp.toPx(); val y = size.height - 5.dp.toPx()
                        val w = size.width - inset * 2
                        drawRoundRect(Color.White.copy(alpha = 0.12f), Offset(inset, y), Size(w, th), CornerRadius(th))
                        drawRoundRect(accent, Offset(inset, y), Size(w * p, th), CornerRadius(th))
                    }
                    else -> Unit
                }
            },
        content = content,
    )
}

/** Builds the perimeter path once per size and reuses it every frame. */
private class RimCache {
    private var key = Triple(0f, 0f, 0f)
    private val path = Path()
    private val seg = Path()
    private val pm = PathMeasure()
    private var len = 0f

    private fun ensure(w: Float, h: Float, r: Float, inset: Float) {
        val k = Triple(w, h, r)
        if (k == key) return
        key = k
        path.reset()
        val rr = minOf(r - inset, h / 2 - inset, w / 2 - inset).coerceAtLeast(0f)
        val l = inset; val t = inset; val rt = w - inset; val b = h - inset
        // Start at top centre, go clockwise, so progress grows from the lens outward.
        path.moveTo(w / 2, t)
        path.lineTo(rt - rr, t)
        path.arcTo(Rect(rt - 2 * rr, t, rt, t + 2 * rr), -90f, 90f, false)
        path.lineTo(rt, b - rr)
        path.arcTo(Rect(rt - 2 * rr, b - 2 * rr, rt, b), 0f, 90f, false)
        path.lineTo(l + rr, b)
        path.arcTo(Rect(l, b - 2 * rr, l + 2 * rr, b), 90f, 90f, false)
        path.lineTo(l, t + rr)
        path.arcTo(Rect(l, t, l + 2 * rr, t + 2 * rr), 180f, 90f, false)
        path.close()
        pm.setPath(path, false)
        len = pm.length
    }

    fun line(d: DrawScope, r: Float, p: Float, color: Color) = with(d) {
        val sw = 1.5.dp.toPx()
        ensure(size.width, size.height, r, sw / 2)
        if (p <= 0f) return
        seg.reset()
        pm.getSegment(0f, len * p, seg, true)
        drawPath(seg, color, style = Stroke(sw, cap = StrokeCap.Round))
    }

    fun ticks(d: DrawScope, r: Float, p: Float, color: Color) = with(d) {
        val sw = 2.4.dp.toPx()
        ensure(size.width, size.height, r, sw / 2)
        val count = (len / 9.5.dp.toPx()).toInt().coerceAtLeast(12)
        val step = len / count
        val tick = (step * 0.12f).coerceAtLeast(1.2f)
        val dim = Color.White.copy(alpha = 0.14f)
        for (i in 0 until count) {
            val s = i * step
            seg.reset()
            pm.getSegment(s, s + tick, seg, true)
            drawPath(seg, if (s / len < p) color else dim, style = Stroke(sw))
        }
    }
}
