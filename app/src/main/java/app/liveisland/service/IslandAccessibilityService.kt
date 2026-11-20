package app.liveisland.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.RectF
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.provider.Settings as SysSettings
import android.view.DragEvent
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import app.liveisland.core.Haptics
import app.liveisland.core.IslandItem
import app.liveisland.core.Kind
import app.liveisland.core.Settings
import app.liveisland.core.SettingsStore
import app.liveisland.engine.IslandEngine
import app.liveisland.engine.Mode
import app.liveisland.engine.SystemActions
import app.liveisland.engine.Timers
import app.liveisland.island.Island
import app.liveisland.island.IslandHost
import app.liveisland.island.IslandStyle
import app.liveisland.island.LensSide
import app.liveisland.shelf.ShelfEntry
import app.liveisland.shelf.ShelfStore
import app.liveisland.sources.BatterySource
import app.liveisland.sources.BluetoothSource
import app.liveisland.sources.PluginReceiver
import app.liveisland.sources.PollingFacts
import app.liveisland.sources.PrivacySource
import app.liveisland.sources.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Hosts the island. An accessibility overlay is the only window type a normal app
 * can draw over the status bar with, which is why every island app needs this permission.
 */
class IslandAccessibilityService : AccessibilityService(), IslandHost, SystemActions {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var store: SettingsStore
    private lateinit var wm: WindowManager
    private var view: ComposeView? = null
    private val owner = OverlayLifecycleOwner()
    private val params = WindowManager.LayoutParams()
    private class Managed(val src: Source, val want: (Settings) -> Boolean) { var on = false }
    private val managed = mutableListOf<Managed>()
    private val powerReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(c: android.content.Context, i: Intent) {
            IslandEngine.lowPower.value = c.getSystemService(android.os.PowerManager::class.java).isPowerSaveMode
        }
    }
    private var privacy: PrivacySource? = null

    private val foreground = MutableStateFlow<String?>(null)
    private val screenOn = MutableStateFlow(true)
    private val shown = MutableStateFlow(true)
    private val screenReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(c: android.content.Context, i: Intent) { screenOn.value = i.action == Intent.ACTION_SCREEN_ON }
    }
    private val fullscreen = MutableStateFlow(false)
    private val lensSpot = MutableStateFlow<LensSpot?>(null)
    private var appCache: List<Pair<String, String>> = emptyList()

    private val settings: Settings get() = store.settings.value

    override fun onServiceConnected() {
        super.onServiceConnected()
        running.value = true
        store = SettingsStore.get(this)
        wm = getSystemService(WindowManager::class.java)
        ShelfStore.load(this)
        IslandEngine.haptic = { strong -> haptic(strong) }
        Timers.onFocusChanged = { on -> setDnd(on) }

        // Each source runs only while something that needs it is switched on. Turning a feature
        // off unregisters its listener completely, so it costs nothing.
        privacy = PrivacySource(this, { settings }, { foreground.value })
        managed += Managed(BatterySource(this) { settings }) { it.features.charging || app.liveisland.core.Fact.BATTERY in it.rest.facts }
        managed += Managed(privacy!!) { it.features.privacy }
        managed += Managed(BluetoothSource(this) { settings }) { it.features.bluetooth }
        managed += Managed(PollingFacts(this, scope) { settings }) { it.rest.mode == app.liveisland.core.RestMode.FACTS || it.features.countdowns }
        managed += Managed(PluginReceiver(this) { settings }) { it.features.plugins }
        scope.launch {
            store.settings.collect { s ->
                val wantAny = s.enabled
                managed.forEach { m ->
                    val want = wantAny && m.want(s)
                    if (want && !m.on) { runCatching { m.src.start() }; m.on = true }
                    else if (!want && m.on) { runCatching { m.src.stop() }; m.on = false }
                }
            }
        }

        // Battery Saver: go still.
        val power = getSystemService(android.os.PowerManager::class.java)
        IslandEngine.lowPower.value = power.isPowerSaveMode
        androidx.core.content.ContextCompat.registerReceiver(this, powerReceiver,
            android.content.IntentFilter(android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED), androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)

        attach()

        // Screen on/off: everything parks while the screen is off.
        val pm = getSystemService(android.os.PowerManager::class.java)
        screenOn.value = pm.isInteractive
        androidx.core.content.ContextCompat.registerReceiver(this, screenReceiver, android.content.IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_SCREEN_OFF)
        }, androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        scope.launch { combine(screenOn, shown) { on, vis -> on && vis }.distinctUntilChanged().collect { IslandEngine.awake.value = it } }

        // Clock for live text. Restarts whenever items, settings or awake state change, then
        // sleeps exactly as long as the screen content allows. Zero wake-ups while asleep.
        scope.launch {
            combine(IslandEngine.items, store.settings, IslandEngine.awake, IslandEngine.lowPower) { _, s, awake, low -> Triple(s, awake, low) }.collectLatest { (s, awake, low) ->
                if (!awake) return@collectLatest
                val rest = s.rest
                val rotate = if (rest.mode == app.liveisland.core.RestMode.FACTS && rest.facts.size > 1) rest.rotateSeconds * 1000L * (if (low) 3 else 1) else null
                val netLive = s.features.netSpeed && app.liveisland.core.Fact.NET_SPEED in rest.facts
                while (true) {
                    val now = System.currentTimeMillis()
                    IslandEngine.now.value = now
                    IslandEngine.sweep(now)
                    delay(IslandEngine.nextTickIn(now, rotate, netLive))
                }
            }
        }
        // show / hide + reposition whenever anything relevant changes
        scope.launch {
            combine(store.settings, foreground, fullscreen, lensSpot) { s, fg, fs, _ ->
                s.enabled && !(s.placement.autoHideFullscreen && fs) && (fg == null || fg !in s.hideInApps)
            }.distinctUntilChanged().collect { show -> shown.value = show; view?.visibility = if (show) View.VISIBLE else View.GONE }
        }
        scope.launch { store.settings.map { it.placement }.distinctUntilChanged().collect { position() } }
        scope.launch { store.settings.map { it.pinnedNote to it.features.notes }.distinctUntilChanged().collect { (note, on) -> pinned(note, on) } }
        scope.launch { appCache = withContext(Dispatchers.IO) { loadApps() } }
    }

    // ---------------- window ----------------

    private fun attach() {
        val v = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent { OverlayContent() }
            setOnApplyWindowInsetsListener { _, insets ->
                // Only read status-bar visibility here. Never move the window from inside this callback.
                if (Build.VERSION.SDK_INT >= 30) fullscreen.value = !insets.isVisible(WindowInsets.Type.statusBars())
                insets
            }
            setOnDragListener(::onDrop)
        }
        owner.create()
        params.apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            val d = resources.displayMetrics.density
            width = (200 * d).toInt()
            height = (40 * d).toInt()
            layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= 30) WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            else WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            title = "Live Island"
        }
        lensSpot.value = Cutout.detect(this, null)
        place()
        runCatching { wm.addView(v, params) }.onSuccess { view = v }
    }

    /** Pins the window so the lens sits exactly on the camera, then applies the user's nudge. */
    private fun place() {
        val spot = lensSpot.value ?: return
        val p = settings.placement
        val d = resources.displayMetrics.density
        val islandH = (34f * p.scale * d)
        val side = spot.side(p.anchor)
        val cx = when (p.anchor) {
            app.liveisland.core.Anchor.LEFT -> if (spot.side(app.liveisland.core.Anchor.AUTO) == LensSide.START) spot.cx else (islandH / 2 + 12 * d).toInt()
            app.liveisland.core.Anchor.RIGHT -> if (spot.side(app.liveisland.core.Anchor.AUTO) == LensSide.END) spot.cx else (spot.screenW - islandH / 2 - 12 * d).toInt()
            app.liveisland.core.Anchor.CENTER -> if (spot.side(app.liveisland.core.Anchor.AUTO) == LensSide.CENTER) spot.cx else spot.screenW / 2
            app.liveisland.core.Anchor.AUTO -> spot.cx
        }
        params.y = (spot.cy - islandH / 2 + p.offsetY * d).toInt().coerceAtLeast(0)
        when (side) {
            LensSide.CENTER -> { params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; params.x = (cx - spot.screenW / 2 + p.offsetX * d).toInt() }
            LensSide.START -> { params.gravity = Gravity.TOP or Gravity.LEFT; params.x = (cx - islandH / 2 + p.offsetX * d).toInt() }
            LensSide.END -> { params.gravity = Gravity.TOP or Gravity.RIGHT; params.x = (spot.screenW - cx - islandH / 2 - p.offsetX * d).toInt() }
        }
        currentSide.value = side
    }

    private fun position() {
        place()
        apply()
    }

    /** Pushes params to the window only when something actually changed. */
    private var applied: List<Int> = emptyList()
    private fun apply() {
        val v = view ?: return
        val now = listOf(params.x, params.y, params.width, params.height, params.gravity, params.flags)
        if (now == applied) return
        applied = now
        runCatching { wm.updateViewLayout(v, params) }
    }

    /**
     * The window is resized once per transition, not every frame: grow it before the island
     * expands, shrink it after the island has finished collapsing. Resizing a window every frame
     * is what made the island flicker.
     */
    private var shrink: kotlinx.coroutines.Job? = null
    private fun onTargetSize(w: Int, h: Int) {
        val pad = (8 * resources.displayMetrics.density).toInt()
        val tw = w + pad * 2
        val th = h + pad
        if (tw > params.width || th > params.height) {
            params.width = maxOf(params.width, tw); params.height = maxOf(params.height, th)
            apply()
        }
        shrink?.cancel()
        shrink = scope.launch {
            delay(if (settings.theme == app.liveisland.core.ThemeId.CLAY) 1200 else 700)
            params.width = tw; params.height = th
            apply()
        }
    }

    private val currentSide = MutableStateFlow(LensSide.CENTER)

    @androidx.compose.runtime.Composable
    private fun OverlayContent() {
        val s by store.settings.collectAsState()
        val items by IslandEngine.items.collectAsState()
        val ui by IslandEngine.ui.collectAsState()
        val facts by IslandEngine.facts.collectAsState()
        val now by IslandEngine.now.collectAsState()
        val side by currentSide.collectAsState()
        val align = when (side) { LensSide.START -> androidx.compose.ui.Alignment.TopStart; LensSide.END -> androidx.compose.ui.Alignment.TopEnd; else -> androidx.compose.ui.Alignment.TopCenter }
        androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSizeCompat(), contentAlignment = align) {
            Island(
                style = IslandStyle.of(s), items = items, ui = ui, facts = facts, rest = s.rest, features = s.features,
                lens = side, now = now, scale = s.placement.scale, host = this@IslandAccessibilityService, onTargetSize = ::onTargetSize,
            )
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        lensSpot.value = Cutout.detect(this, null)
        position()
    }

    // ---------------- IslandHost ----------------

    override val sys: SystemActions get() = this

    override fun setKeyboard(on: Boolean) {
        val v = view ?: return
        val f = params.flags
        params.flags = if (on) f and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv() else f or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        if (params.flags != f && v.isAttachedToWindow) apply()
    }

    override fun haptic(strong: Boolean) {
        val level = settings.features.haptics
        if (level == Haptics.OFF) return
        val c = when {
            strong || level == Haptics.STRONG -> if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
            else -> HapticFeedbackConstants.CLOCK_TICK
        }
        view?.performHapticFeedback(c)
    }

    private fun uriFor(e: ShelfEntry) = ShelfStore.file(this, e)?.let { FileProvider.getUriForFile(this, "$packageName.shelf", it) }

    override fun shareShelf(e: ShelfEntry) {
        val send = Intent(Intent.ACTION_SEND).apply {
            if (e.isText) { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, e.text) }
            else { type = e.type; putExtra(Intent.EXTRA_STREAM, uriFor(e)); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        IslandEngine.setMode(Mode.AUTO)
        startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }

    override fun dragShelf(e: ShelfEntry) {
        val v = view ?: return
        val clip = if (e.isText) ClipData.newPlainText(e.label, e.text)
        else ClipData(e.label, arrayOf(e.type), ClipData.Item(uriFor(e)))
        haptic(true)
        v.startDragAndDrop(clip, LabelShadow(e.label, resources.displayMetrics.density), null,
            View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ)
    }

    /** Things dragged onto the island land on the shelf. */
    private fun onDrop(v: View, ev: DragEvent): Boolean {
        if (!settings.features.shelf) return false
        return when (ev.action) {
            DragEvent.ACTION_DRAG_STARTED -> ev.clipDescription?.let { it.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) || it.mimeTypeCount > 0 } ?: false
            DragEvent.ACTION_DROP -> {
                val clip = ev.clipData ?: return false
                for (i in 0 until clip.itemCount) {
                    val item = clip.getItemAt(i)
                    val uri = item.uri
                    if (uri != null) ShelfStore.addUri(this, uri, ev.clipDescription?.getMimeType(0))
                    else item.text?.toString()?.let { ShelfStore.addText(this, it) }
                }
                haptic(true)
                IslandEngine.setMode(Mode.SHELF)
                true
            }
            else -> true
        }
    }

    private class LabelShadow(val label: String, val d: Float) : View.DragShadowBuilder() {
        private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF000000.toInt() }
        private val fg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFEEECE7.toInt(); textSize = 13 * d }
        private val text = label.take(24)
        override fun onProvideShadowMetrics(size: Point, touch: Point) {
            size.set((fg.measureText(text) + 28 * d).toInt(), (34 * d).toInt()); touch.set(size.x / 2, size.y / 2)
        }
        override fun onDrawShadow(c: Canvas) {
            c.drawRoundRect(RectF(0f, 0f, c.width.toFloat(), c.height.toFloat()), 17 * d, 17 * d, bg)
            c.drawText(text, 14 * d, c.height / 2f + 4.5f * d, fg)
        }
    }

    // ---------------- SystemActions ----------------

    private var torch = false
    override fun torch(): Boolean = runCatching {
        val cm = getSystemService(CameraManager::class.java)
        val id = cm.cameraIdList.first { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
        torch = !torch
        privacy?.torchOn = torch
        cm.setTorchMode(id, torch)
        torch
    }.getOrDefault(false)

    private fun go(i: Intent) = runCatching { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.let { IslandEngine.setMode(Mode.AUTO) }
    override fun wifiPanel() = go(Intent(SysSettings.Panel.ACTION_WIFI))
    override fun internetPanel() = go(Intent(SysSettings.Panel.ACTION_INTERNET_CONNECTIVITY))
    override fun bluetoothSettings() = go(Intent(SysSettings.ACTION_BLUETOOTH_SETTINGS))
    override fun launch(pkg: String) { packageManager.getLaunchIntentForPackage(pkg)?.let(::go) }
    override fun pinNote(text: String) = store.update { it.copy(pinnedNote = text) }
    override fun openShelf() = IslandEngine.setMode(Mode.SHELF)
    override fun apps() = appCache

    private fun loadApps(): List<Pair<String, String>> = runCatching {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(i, 0).map { it.loadLabel(packageManager).toString() to it.activityInfo.packageName }
            .distinctBy { it.second }.sortedBy { it.first.lowercase() }
    }.getOrDefault(emptyList())

    private fun pinned(note: String?, on: Boolean) {
        if (note.isNullOrBlank() || !on) { IslandEngine.remove(NOTE_ID); return }
        IslandEngine.post(
            IslandItem(
                NOTE_ID, Kind.NOTE, title = note,
                actions = listOf(app.liveisland.core.IslandAction("unpin", "Unpin", app.liveisland.island.Glyph.CLOSE) { store.update { it.copy(pinnedNote = null) } }),
            )
        )
    }

    private fun setDnd(on: Boolean) {
        val nm = getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) return
        runCatching { nm.setInterruptionFilter(if (on) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL) }
    }

    // ---------------- accessibility ----------------

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: return
            if (pkg != packageName && pkg != "com.android.systemui") foreground.value = pkg
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        running.value = false
        runCatching { unregisterReceiver(screenReceiver) }
        runCatching { unregisterReceiver(powerReceiver) }
        managed.filter { it.on }.forEach { runCatching { it.src.stop() } }
        managed.clear()
        view?.let { runCatching { wm.removeView(it) } }
        view = null
        owner.destroy()
        IslandEngine.haptic = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTE_ID = "note"
        val running = MutableStateFlow(false)
    }
}

private fun androidx.compose.ui.Modifier.fillMaxSizeCompat() = this.fillMaxSize()
