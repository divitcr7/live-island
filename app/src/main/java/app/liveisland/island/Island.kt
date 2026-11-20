package app.liveisland.island

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.liveisland.core.Fact
import app.liveisland.core.FactValue
import app.liveisland.core.Features
import app.liveisland.core.IslandItem
import app.liveisland.core.Kind
import app.liveisland.core.RestMode
import app.liveisland.core.RestSettings
import app.liveisland.engine.Commands
import app.liveisland.engine.IslandEngine
import app.liveisland.engine.IslandUi
import app.liveisland.engine.Mode
import app.liveisland.engine.Suggestion
import app.liveisland.engine.SystemActions
import app.liveisland.engine.Timers
import app.liveisland.shelf.ShelfEntry
import app.liveisland.shelf.ShelfStore
import kotlin.math.abs

/** Things only the real overlay can do. Previews pass null. */
interface IslandHost {
    val sys: SystemActions
    fun setKeyboard(on: Boolean)
    fun haptic(strong: Boolean = false)
    fun shareShelf(e: ShelfEntry)
    fun dragShelf(e: ShelfEntry)
}

private sealed interface Face {
    data object Ring : Face
    data class Rest(val fact: FactValue?) : Face
    data class Item(val id: String, val kind: Kind, val expanded: Boolean) : Face
    data object Command : Face
    data object Shelf : Face
}

/**
 * The whole island: main pill, satellites, gestures and every transition.
 * [scale] multiplies every size so users can make it bigger or smaller.
 */
@Composable
fun Island(
    style: IslandStyle,
    items: Map<String, IslandItem>,
    ui: IslandUi,
    facts: Map<Fact, FactValue>,
    rest: RestSettings,
    features: Features,
    lens: LensSide,
    now: Long,
    scale: Float = 1f,
    host: IslandHost? = null,
    drawLens: Boolean = false,
    onModeChange: (Mode) -> Unit = IslandEngine::setMode,
    modifier: Modifier = Modifier,
) {
    val base = LocalDensity.current
    androidx.compose.runtime.CompositionLocalProvider(LocalDensity provides Density(base.density * scale, 1f)) {
        IslandBody(style, items, ui, facts, rest, features, lens, now, host, drawLens, onModeChange, modifier)
    }
}

@Composable
private fun IslandBody(
    style: IslandStyle, items: Map<String, IslandItem>, ui: IslandUi, facts: Map<Fact, FactValue>,
    rest: RestSettings, features: Features, lens: LensSide, now: Long, host: IslandHost?, drawLens: Boolean,
    onModeChange: (Mode) -> Unit, modifier: Modifier,
) {
    val visible = items.filterValues { it.kind != Kind.SHELF }
    val primary = IslandEngine.primary(visible, ui)
    var cmd by remember { mutableStateOf("") }
    val suggestions: List<Suggestion> = remember(cmd, host) { host?.let { Commands.suggest(cmd, it.sys) } ?: emptyList() }
    val shelf by ShelfStore.entries.collectAsState()

    // ---- decide what the pill shows ----
    val ringOnly = rest.mode == RestMode.RING
    val fact = if (ringOnly) null else pickFact(facts, rest.facts, now, rest.rotateSeconds)
    val face: Face = when {
        ui.mode == Mode.COMMAND -> Face.Command
        ui.mode == Mode.SHELF -> Face.Shelf
        primary == null -> if (ui.mode == Mode.SHRUNK || ringOnly || fact == null) Face.Ring else Face.Rest(fact)
        ui.mode == Mode.SHRUNK && !naturallyBig(primary) -> Face.Ring
        else -> Face.Item(primary.id, primary.kind, ui.mode == Mode.EXPANDED && hasExpanded(primary))
    }
    val dims = when (face) {
        Face.Ring -> Dims(40.dp, COMPACT_H.dp, false)
        is Face.Rest -> Dims(restWidth(false), COMPACT_H.dp, false)
        Face.Command -> Dims(320.dp, (COMPACT_H + 44 + 10 + suggestions.size * 38 + 10 + 46 + 14).dp, true)
        Face.Shelf -> Dims(320.dp, if (shelf.isEmpty()) 104.dp else 176.dp, true)
        is Face.Item -> if (face.expanded) expandedDims(primary!!) else compactDims(primary!!)
    }
    val accent = when {
        primary != null && face is Face.Item -> style.accent(primary.kind, primary.artAccent)
        primary != null && face == Face.Ring -> style.accent(primary.kind, primary.artAccent)
        else -> style.palette.neutral
    }
    val progress = when (face) {
        is Face.Item -> primary?.let { liveProgress(it, now) }
        Face.Ring -> primary?.let { liveProgress(it, now) } ?: 1f
        else -> null
    }
    val ringAccent = if (face == Face.Ring && primary == null) accent.copy(alpha = 0.45f) else accent

    val w by animateDpAsState(dims.w, style.spring(), label = "w")
    val h by animateDpAsState(dims.h, style.spring(), label = "h")
    val r by animateDpAsState(style.radius(dims.h, dims.expanded), style.spring(), label = "r")

    val sats = if (features.satellites && face is Face.Item && !face.expanded && !naturallyBig(primary!!))
        IslandEngine.ordered(visible).filter { it.id != primary.id }.take(if (lens == LensSide.CENTER) 1 else 2) else emptyList()

    // ---- gestures ----
    val cur by rememberUpdatedState(Triple(face, primary, ui.mode))
    fun change(m: Mode) { if (m != ui.mode) { host?.haptic(false); onModeChange(m) } }
    fun tap() {
        val (f, p, mode) = cur
        when {
            mode == Mode.SHRUNK -> change(Mode.AUTO)
            mode == Mode.EXPANDED || mode == Mode.COMMAND || mode == Mode.SHELF -> change(Mode.AUTO)
            p != null && naturallyBig(p) -> p.onOpen?.invoke()
            p != null && hasExpanded(p) -> change(Mode.EXPANDED)
            f is Face.Rest || f == Face.Ring -> if (features.commandBar) change(Mode.COMMAND)
        }
    }
    fun doubleTap() {
        val p = cur.second ?: return
        (p.actions.firstOrNull { it.primary } ?: p.actions.firstOrNull())?.run?.invoke()
        host?.haptic(true)
    }
    fun swipe(dx: Float, dy: Float) {
        val mode = cur.third
        if (abs(dx) > abs(dy) && abs(dx) > 40f) { IslandEngine.cycle(if (dx > 0) 1 else -1); host?.haptic(false); return }
        if (dy > 24f) change(when { mode == Mode.SHRUNK -> Mode.AUTO; cur.second?.let { hasExpanded(it) } == true -> Mode.EXPANDED; features.commandBar -> Mode.COMMAND; else -> mode })
        else if (dy < -24f) {
            val p = cur.second
            if (p != null && naturallyBig(p) && mode == Mode.AUTO) { IslandEngine.remove(p.id); return }
            change(if (mode == Mode.AUTO) Mode.SHRUNK else Mode.AUTO)
        }
    }
    val gestures = if (host == null) Modifier else Modifier
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = { tap() },
                onDoubleTap = { doubleTap() },
                onLongPress = { if (features.commandBar) { host.haptic(true); onModeChange(Mode.COMMAND) } },
            )
        }
        .pointerInput(Unit) {
            var dx = 0f; var dy = 0f
            detectDragGestures(
                onDragStart = { dx = 0f; dy = 0f },
                onDragEnd = { swipe(dx / density, dy / density) },
                onDrag = { c, d -> c.consume(); dx += d.x; dy += d.y },
            )
        }

    LaunchedEffect(face == Face.Command) { host?.setKeyboard(face == Face.Command); if (face != Face.Command) cmd = "" }

    // ---- layout: pill + satellites, keeping the pill on the lens ----
    val satW = COMPACT_H.dp + 6.dp
    ProvideIsland(style, lens, accent) {
        Row(modifier.wrapContentSize(), verticalAlignment = Alignment.Top) {
            val satRow: @Composable (List<IslandItem>) -> Unit = { list ->
                list.forEach { s ->
                    Spacer(Modifier.width(6.dp))
                    CompositionLocalProviderAccent(style.accent(s.kind, s.artAccent)) {
                        SatelliteView(s, now, if (host != null) ({ IslandEngine.focus(s.id) }) else null)
                    }
                }
            }
            when (lens) {
                LensSide.END -> { satRowReversed(sats, now, style, host); }
                LensSide.CENTER -> {
                    // balance the right-hand satellite with empty space so the pill stays centred on the camera
                    if (sats.isNotEmpty()) Spacer(Modifier.width(satW))
                }
                LensSide.START -> Unit
            }
            IslandSurface(style, w, h, r, if (face == Face.Ring) ringAccent else accent, progress, gestures) {
                Box(
                    Modifier.requiredSize(dims.w, dims.h).align(
                        when (lens) { LensSide.START -> Alignment.TopStart; LensSide.END -> Alignment.TopEnd; LensSide.CENTER -> Alignment.TopCenter }
                    )
                ) {
                    AnimatedContent(
                        targetState = face,
                        transitionSpec = {
                            (fadeIn(tween(220, 70)) + scaleIn(tween(260, 40), initialScale = 0.94f)) togetherWith fadeOut(tween(110))
                        },
                        contentKey = { f -> if (f is Face.Rest) "rest" else f },
                        label = "face",
                    ) { f ->
                        Box(Modifier.requiredSize(dims.w, dims.h)) {
                            when (f) {
                                Face.Ring -> Unit
                                is Face.Rest -> RestView(fact)
                                is Face.Item -> {
                                    val it = items[f.id]
                                    if (it != null) {
                                        if (f.expanded) ExpandedView(it, now)
                                        else if (naturallyBig(it)) BigView(it, now)
                                        else CompactView(it, now)
                                    }
                                }
                                Face.Command -> CommandView(cmd, { cmd = it }, suggestions, features, host) { onModeChange(Mode.AUTO) }
                                Face.Shelf -> ShelfView(shelf, host)
                            }
                        }
                    }
                }
                if (drawLens) Lens(lens)
            }
            when (lens) {
                LensSide.START -> satRow(sats)
                LensSide.CENTER -> satRow(sats.take(1))
                LensSide.END -> Unit
            }
        }
    }
}

@Composable
private fun satRowReversed(sats: List<IslandItem>, now: Long, style: IslandStyle, host: IslandHost?) {
    sats.reversed().forEach { s ->
        CompositionLocalProviderAccent(style.accent(s.kind, s.artAccent)) { SatelliteView(s, now, host?.let { { IslandEngine.focus(s.id) } }) }
        Spacer(Modifier.width(6.dp))
    }
}

@Composable
private fun CompositionLocalProviderAccent(c: Color, content: @Composable () -> Unit) =
    androidx.compose.runtime.CompositionLocalProvider(LocalAccent provides c, content = content)

/** Camera hole drawn in previews only; on a real phone the hardware is there. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.Lens(lens: LensSide) {
    val x = when (lens) { LensSide.START -> Alignment.TopStart; LensSide.END -> Alignment.TopEnd; LensSide.CENTER -> Alignment.TopCenter }
    val pad = (COMPACT_H.dp - 12.dp) / 2
    Box(
        Modifier.align(x).padding(top = pad, start = if (lens == LensSide.START) pad else 0.dp, end = if (lens == LensSide.END) pad else 0.dp)
            .size(12.dp).clip(CircleShape).background(Color(0xFF121216))
    ) { Box(Modifier.offset(3.dp, 3.dp).size(3.dp).clip(CircleShape).background(Color(0xFF2A3348))) }
}

// ---------- command bar ----------

@Composable
private fun CommandView(
    text: String, onText: (String) -> Unit, suggestions: List<Suggestion>, features: Features,
    host: IslandHost?, close: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    fun run(s: Suggestion) { s.run(); close() }
    Card("Command", "hold to open") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.07f)).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                G(Glyph.SEARCH, 15.dp, TextLo)
                Box(Modifier.weight(1f)) {
                    if (text.isEmpty()) T("timer 10 · focus 25 · note … · app", 13.sp, TextFaint)
                    BasicTextField(
                        text, onText, Modifier.fillMaxWidth().focusRequester(focus), singleLine = true,
                        textStyle = TextStyle(color = TextHi, fontSize = 14.sp, fontFamily = LocalStyle.current.fonts.text),
                        cursorBrush = SolidColor(LocalAccent.current),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { suggestions.firstOrNull()?.let(::run) }),
                    )
                }
            }
            suggestions.forEach { s ->
                Row(
                    Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(8.dp)).clickable { run(s) }.padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    G(s.glyph, 15.dp, LocalAccent.current)
                    T(s.title, 13.5.sp, modifier = Modifier.weight(1f))
                    T(s.hint, 11.sp, TextLo)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val sys = host?.sys
                Quick(Glyph.TORCH) { sys?.torch() }
                Quick(Glyph.WIFI) { sys?.wifiPanel(); close() }
                Quick(Glyph.BT) { sys?.bluetoothSettings(); close() }
                if (features.focus) Quick(Glyph.FOCUS) { Timers.startFocus(25 * 60_000L); close() }
                if (features.shelf) Quick(Glyph.TRAY) { IslandEngine.setMode(Mode.SHELF) }
                Quick(Glyph.HOURGLASS) { Timers.startTimer(5 * 60_000L); close() }
            }
        }
    }
}

@Composable
private fun Quick(g: Glyph, onClick: () -> Unit) {
    val tight = LocalStyle.current.corners == app.liveisland.core.Corners.TIGHT
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(if (tight) 10.dp else 14.dp)).background(Color.White.copy(alpha = 0.07f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { G(g, 18.dp, Color(0xFFCFCDC7)) }
}

// ---------- shelf ----------

@Composable
private fun ShelfView(entries: List<ShelfEntry>, host: IslandHost?) {
    val ctx = LocalContext.current
    Card("Shelf · ${entries.size}", if (entries.isEmpty()) "" else "tap to share · hold to drag") {
        if (entries.isEmpty()) {
            T("Share anything to Live Island, or drop it here. It waits until you need it.", 13.sp, TextLo, maxLines = 3)
        } else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                entries.take(4).forEach { e ->
                    Column(
                        Modifier.weight(1f).height(84.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.06f))
                            .pointerInput(e.id) { detectTapGestures(onTap = { host?.shareShelf(e) }, onLongPress = { host?.dragShelf(e) }) }
                            .padding(8.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val thumb = remember(e.id) { if (e.isImage) loadThumb(ShelfStore.file(ctx, e)?.path) else null }
                        if (thumb != null) Image(thumb, null, Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        else G(if (e.isText) Glyph.COPY else Glyph.BOX, 16.dp, TextLo)
                        T(e.label, 11.sp, Color(0xFFBDBBB4))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill("Clear", Glyph.CLOSE) { ShelfStore.clear(ctx) }
            }
        }
    }
}

private fun loadThumb(path: String?): ImageBitmap? = path?.let {
    runCatching {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(it, o)
        var s = 1
        while (o.outWidth / s > 240 || o.outHeight / s > 240) s *= 2
        BitmapFactory.decodeFile(it, BitmapFactory.Options().apply { inSampleSize = s })?.asImageBitmap()
    }.getOrNull()
}


@Suppress("unused") private val unusedDp: Dp = 0.dp
