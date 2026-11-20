package app.liveisland.sources

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import app.liveisland.core.Fact
import app.liveisland.core.FactValue
import app.liveisland.core.IslandItem
import app.liveisland.core.Kind
import app.liveisland.core.Settings
import app.liveisland.engine.IslandEngine
import app.liveisland.island.Glyph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** Something that feeds the engine while the overlay is alive. */
interface Source {
    fun start()
    fun stop()
}

// ---------------- battery & charging ----------------

class BatterySource(private val ctx: Context, private val settings: () -> Settings) : Source {
    private val bm = ctx.getSystemService(BatteryManager::class.java)
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                Intent.ACTION_POWER_CONNECTED -> Handler(Looper.getMainLooper()).postDelayed({ showCharging() }, 600)
                Intent.ACTION_POWER_DISCONNECTED -> IslandEngine.remove(ID)
                Intent.ACTION_BATTERY_CHANGED -> updateFact(i)
            }
        }
    }

    override fun start() {
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED); addAction(Intent.ACTION_POWER_DISCONNECTED); addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        ContextCompat.registerReceiver(ctx, receiver, f, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun stop() = runCatching { ctx.unregisterReceiver(receiver) }.let { }

    private fun sticky() = ContextCompat.registerReceiver(ctx, null, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)

    private fun percent(i: Intent?): Int {
        val l = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val s = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        return if (l < 0) bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) else l * 100 / s
    }

    private fun updateFact(i: Intent) {
        val pct = percent(i)
        val plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        IslandEngine.fact(Fact.BATTERY, FactValue(if (plugged) Glyph.BOLT else Glyph.BATTERY, "$pct%", if (plugged) "Charging" else "Battery"))
    }

    private fun showCharging() {
        if (!settings().features.charging) return
        val i = sticky()
        val pct = percent(i)
        val remaining = bm.computeChargeTimeRemaining()
        val sub = when {
            settings().features.chargingStats -> watts(i)
            remaining > 0 -> "full in ${fmtDur(remaining)}"
            else -> "charging"
        }
        IslandEngine.post(
            IslandItem(
                id = ID, kind = Kind.CHARGING, title = if (settings().features.chargingStats && remaining > 0) "full in ${fmtDur(remaining)}" else "Charging",
                value = "$pct%", subtitle = sub, glyph = Glyph.BOLT, progress = pct / 100f,
                expiresAt = System.currentTimeMillis() + 4_500,
            )
        )
    }

    /** Real-time charging power: current (µA, sign varies by OEM) × voltage (mV). */
    private fun watts(i: Intent?): String {
        val ua = abs(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW).toLong())
        val mv = (i?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0).toLong()
        val temp = (i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        // some phones report mA instead of µA
        val amps = if (ua > 20_000) ua / 1_000_000f else ua / 1_000f
        val w = amps * mv / 1000f
        return "%.1f W · %.0f°C".format(w, temp)
    }

    companion object { const val ID = "charging" }
}

fun fmtDur(ms: Long): String {
    val m = ms / 60_000
    return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m"
}

// ---------------- camera & microphone ----------------

class PrivacySource(private val ctx: Context, private val settings: () -> Settings, private val foreground: () -> String?) : Source {
    private val cams = ctx.getSystemService(CameraManager::class.java)
    private val audio = ctx.getSystemService(AudioManager::class.java)
    private val busy = mutableSetOf<String>()
    private var mic = false
    /** The torch makes cameras "unavailable" too; ignore while we hold it. */
    var torchOn = false
    private val main = Handler(Looper.getMainLooper())

    private val camCb = object : CameraManager.AvailabilityCallback() {
        override fun onCameraUnavailable(id: String) { busy += id; publish() }
        override fun onCameraAvailable(id: String) { busy -= id; publish() }
    }
    private val micCb = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>) { mic = configs.isNotEmpty(); publish() }
    }

    override fun start() {
        cams.registerAvailabilityCallback(camCb, main)
        audio.registerAudioRecordingCallback(micCb, main)
    }

    override fun stop() {
        cams.unregisterAvailabilityCallback(camCb)
        audio.unregisterAudioRecordingCallback(micCb)
        IslandEngine.remove(ID)
    }

    private fun publish() {
        val cam = busy.isNotEmpty() && !torchOn
        if (!settings().features.privacy || (!cam && !mic)) { IslandEngine.remove(ID); return }
        val what = when { cam && mic -> "Camera + mic"; cam -> "Camera"; else -> "Microphone" }
        val app = foreground()?.let { appLabel(ctx, it) } ?: ""
        IslandEngine.post(IslandItem(ID, Kind.PRIVACY, title = what, subtitle = app, glyph = if (cam) Glyph.CAMERA else Glyph.MIC))
    }

    companion object { const val ID = "privacy" }
}

fun appLabel(ctx: Context, pkg: String): String? = runCatching {
    val pm = ctx.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
}.getOrNull()

// ---------------- bluetooth ----------------

class BluetoothSource(private val ctx: Context, private val settings: () -> Settings) : Source {
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (!settings().features.bluetooth) return
            val dev: BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33) i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            else @Suppress("DEPRECATION") i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            val name = dev?.let { d -> runCatching { if (canConnect()) d.name else null }.getOrNull() } ?: "Bluetooth device"
            when (i.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> show(name, null)
                BATTERY_CHANGED -> {
                    val lvl = i.getIntExtra(EXTRA_BATTERY, -1)
                    if (lvl >= 0 && IslandEngine.items.value.containsKey(ID)) show(name, lvl)
                }
            }
        }
    }

    private fun canConnect() = Build.VERSION.SDK_INT < 31 ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun show(name: String, battery: Int?) {
        IslandEngine.post(
            IslandItem(
                ID, Kind.BLUETOOTH, title = "Connected", subtitle = battery?.let { "$it% battery" } ?: "connected",
                value = name.take(16), glyph = Glyph.BT, progress = battery?.div(100f),
                expiresAt = System.currentTimeMillis() + 4_000,
            )
        )
    }

    override fun start() {
        val f = IntentFilter().apply { addAction(BluetoothDevice.ACTION_ACL_CONNECTED); addAction(BATTERY_CHANGED) }
        ContextCompat.registerReceiver(ctx, receiver, f, ContextCompat.RECEIVER_EXPORTED)
    }

    override fun stop() = runCatching { ctx.unregisterReceiver(receiver) }.let { }

    companion object {
        const val ID = "bluetooth"
        // Hidden-but-stable framework broadcast; most phones send it for headsets.
        const val BATTERY_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        const val EXTRA_BATTERY = "android.bluetooth.device.extra.BATTERY_LEVEL"
    }
}

// ---------------- facts that need polling ----------------

class PollingFacts(private val ctx: Context, private val scope: CoroutineScope, private val settings: () -> Settings) : Source {
    private var running = false
    private val dateFmt = SimpleDateFormat("EEE d", Locale.getDefault())

    override fun start() {
        running = true
        scope.launch { netLoop() }
        scope.launch { slowLoop() }
    }

    override fun stop() { running = false }

    private suspend fun netLoop() {
        var lastRx = TrafficStats.getTotalRxBytes(); var lastTx = TrafficStats.getTotalTxBytes()
        while (running && scope.isActive) {
            delay(1000)
            val s = settings()
            if (!s.features.netSpeed || Fact.NET_SPEED !in s.rest.facts) { IslandEngine.fact(Fact.NET_SPEED, null); continue }
            val rx = TrafficStats.getTotalRxBytes(); val tx = TrafficStats.getTotalTxBytes()
            val down = (rx - lastRx).coerceAtLeast(0); val up = (tx - lastTx).coerceAtLeast(0)
            lastRx = rx; lastTx = tx
            IslandEngine.fact(Fact.NET_SPEED, FactValue(Glyph.DOWN, rate(down), "↑ ${rate(up)}"))
        }
    }

    private suspend fun slowLoop() {
        while (running && scope.isActive) {
            val s = settings()
            val now = System.currentTimeMillis()
            IslandEngine.fact(Fact.DATE, FactValue(Glyph.CAL, dateFmt.format(Date(now)).substringAfter(' '), dateFmt.format(Date(now)).substringBefore(' ')))
            IslandEngine.fact(Fact.NEXT_EVENT, if (Fact.NEXT_EVENT in s.rest.facts) nextEvent(now) else null)
            val cd = if (s.features.countdowns) s.countdowns.filter { it.atEpochMs > now }.minByOrNull { it.atEpochMs } else null
            IslandEngine.fact(Fact.COUNTDOWN, cd?.let { FactValue(Glyph.FLAG, shortUntil(it.atEpochMs - now), it.title) })
            IslandEngine.fact(Fact.NOTE, s.pinnedNote?.takeIf { s.features.notes }?.let { FactValue(Glyph.PIN, "Note", it) })
            delay(30_000)
        }
    }

    private fun nextEvent(now: Long): FactValue? {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) return null
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, now); ContentUris.appendId(it, now + 24 * 3_600_000L)
        }.build()
        return runCatching {
            ctx.contentResolver.query(
                uri, arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.END, CalendarContract.Instances.ALL_DAY),
                "${CalendarContract.Instances.ALL_DAY} = 0 AND ${CalendarContract.Instances.END} > ?", arrayOf(now.toString()),
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { c ->
                if (!c.moveToFirst()) return@use null
                val title = c.getString(0) ?: "Event"
                val begin = c.getLong(1)
                FactValue(Glyph.CAL, if (begin <= now) "now" else shortUntil(begin - now), title)
            }
        }.getOrNull()
    }

    companion object {
        fun rate(bps: Long): String = when {
            bps >= 1_000_000 -> "%.1f MB/s".format(bps / 1_000_000f)
            bps >= 1_000 -> "${bps / 1000} KB/s"
            else -> "$bps B/s"
        }

        fun shortUntil(ms: Long): String {
            val m = ms / 60_000
            return when {
                m < 60 -> "${m.coerceAtLeast(1)}m"
                m < 48 * 60 -> "${m / 60}h ${if (m % 60 > 0) "${m % 60}m" else ""}".trim()
                else -> "${m / (60 * 24)}d"
            }
        }
    }
}
