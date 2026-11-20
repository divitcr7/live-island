package app.liveisland.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.liveisland.island.Glyph
import kotlin.math.roundToInt

private val R8 = RoundedCornerShape(8.dp)
private val R12 = RoundedCornerShape(12.dp)

@Composable
fun Txt(text: String, style: TextStyle = P.t.body, color: Color = P.c.fg, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) =
    BasicText(text, modifier, style.copy(color = color), maxLines = maxLines)

@Composable
fun Label(text: String, modifier: Modifier = Modifier, color: Color = P.c.muted) =
    BasicText(text.uppercase(), modifier, P.t.label.copy(color = color))

@Composable
fun Icon(g: Glyph, size: Dp = 18.dp, tint: Color = P.c.fg, modifier: Modifier = Modifier) =
    Image(rememberVectorPainter(g.vector), null, modifier.size(size), colorFilter = ColorFilter.tint(tint))

@Composable
fun Rule(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().height(1.dp).background(P.c.line))

private fun Modifier.press(onClick: (() -> Unit)?, role: Role = Role.Button) =
    if (onClick == null) this else clickable(interactionSource = null, indication = null, role = role, onClick = onClick)

/** Square-shouldered switch. Ink when on. */
@Composable
fun Switch(on: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = P.c
    val x by animateDpAsState(if (on) 20.dp else 2.dp, spring(dampingRatio = 0.8f, stiffness = 700f), label = "knob")
    val track by animateColorAsState(if (on) c.fg else c.line, label = "track")
    Box(
        modifier.size(44.dp, 26.dp).clip(R8).background(track).press({ onChange(!on) }, Role.Switch),
    ) {
        Box(Modifier.offset(x = x, y = 2.dp).size(22.dp).clip(RoundedCornerShape(6.dp)).background(if (on) c.inv else c.raised))
    }
}

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, swatches: List<Color> = emptyList(), glyph: Glyph? = null) {
    val c = P.c
    val bg by animateColorAsState(if (selected) c.fg else c.surface, label = "chip")
    val fg = if (selected) c.inv else c.fg
    Row(
        modifier.clip(R8).background(bg).border(1.dp, if (selected) c.fg else c.line, R8).press(onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (swatches.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            swatches.forEach { Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(it)) }
        }
        glyph?.let { Icon(it, 14.dp, fg) }
        Txt(label, P.t.small.copy(fontSize = P.t.small.fontSize), fg)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, swatches: (T) -> List<Color> = { emptyList() }) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { o -> Chip(label(o), o == selected, { onSelect(o) }, swatches = swatches(o)) }
    }
}

@Composable
fun Button(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, filled: Boolean = true, glyph: Glyph? = null) {
    val c = P.c
    Row(
        modifier.clip(R12).background(if (filled) c.fg else Color.Transparent).border(1.dp, if (filled) c.fg else c.line, R12)
            .press(onClick).padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        glyph?.let { Icon(it, 16.dp, if (filled) c.inv else c.fg) }
        Txt(label, P.t.heading.copy(fontSize = P.t.body.fontSize), if (filled) c.inv else c.fg)
    }
}

/** A settings row. Everything is one tap; the trailing slot shows the current state. */
@Composable
fun Entry(
    title: String, subtitle: String? = null, glyph: Glyph? = null, onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = { if (onClick != null) Icon(Glyph.CHEVRON, 16.dp, P.c.dim) },
) {
    Row(
        Modifier.fillMaxWidth().press(onClick).padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        glyph?.let {
            Box(Modifier.size(36.dp).clip(R12).background(P.c.surface).border(1.dp, P.c.line, R12), contentAlignment = Alignment.Center) { Icon(it, 17.dp) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Txt(title, P.t.heading)
            subtitle?.let { Txt(it, P.t.small, P.c.muted) }
        }
        trailing()
    }
}

@Composable
fun ToggleEntry(title: String, subtitle: String? = null, on: Boolean, glyph: Glyph? = null, onChange: (Boolean) -> Unit) =
    Entry(title, subtitle, glyph, onClick = { onChange(!on) }) { Switch(on, onChange) }

/** A choice with its options laid out underneath. */
@Composable
fun Choice(title: String, note: String? = null, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Txt(title, P.t.heading)
        content()
        note?.let { Txt(it, P.t.small, P.c.muted) }
    }
}

@Composable
fun Section(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().padding(top = 22.dp)) {
        Label(label)
        Spacer(Modifier.height(6.dp))
        content()
    }
}

/** Slider with a squared thumb; reports values in [range]. */
@Composable
fun Slider(value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit, modifier: Modifier = Modifier, steps: Float = 0.01f) {
    val c = P.c
    val change by rememberUpdatedState(onChange)
    BoxWithConstraints(modifier.fillMaxWidth().height(32.dp)) {
        val mw = maxWidth
        val wPx = with(LocalDensity.current) { (mw - 22.dp).toPx() }
        val frac = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
        fun set(px: Float) {
            val f = (px / wPx).coerceIn(0f, 1f)
            val v = range.start + f * (range.endInclusive - range.start)
            change((v / steps).roundToInt() * steps)
        }
        val thumbPx = with(LocalDensity.current) { 11.dp.toPx() }
        Box(
            Modifier.fillMaxWidth().height(32.dp)
                .pointerInput(wPx) { detectTapGestures { set(it.x - thumbPx) } }
                .pointerInput(wPx) { detectDragGestures { ch, _ -> ch.consume(); set(ch.position.x - thumbPx) } },
        ) {
            Box(Modifier.align(Alignment.CenterStart).padding(horizontal = 11.dp).fillMaxWidth().height(4.dp).clip(CircleShape).background(c.line))
            Box(Modifier.align(Alignment.CenterStart).padding(start = 11.dp).width(mw * frac - 22.dp * frac).height(4.dp).clip(CircleShape).background(c.fg))
            Box(
                Modifier.align(Alignment.CenterStart).offset { IntOffset((wPx * frac).roundToInt(), 0) }
                    .size(22.dp).clip(RoundedCornerShape(7.dp)).background(c.fg).padding(5.dp).clip(RoundedCornerShape(3.dp)).background(c.inv)
            )
        }
    }
}

@Composable
fun Field(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, glyph: Glyph? = null) {
    val c = P.c
    Row(
        modifier.fillMaxWidth().clip(R12).background(c.surface).border(1.dp, c.line, R12).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        glyph?.let { Icon(it, 16.dp, c.muted) }
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Txt(placeholder, P.t.body, c.dim)
            BasicTextField(value, onChange, Modifier.fillMaxWidth(), textStyle = P.t.body.copy(color = c.fg), cursorBrush = SolidColor(c.fg), singleLine = true)
        }
    }
}

/** Status dot + text, e.g. "Running". */
@Composable
fun Status(text: String, ok: Boolean) {
    val c = P.c
    Row(
        Modifier.clip(CircleShape).background(c.surface).border(1.dp, c.line, CircleShape).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(if (ok) c.ok else c.warn))
        Txt(text, P.t.small, c.fg)
    }
}
