package app.liveisland.sources

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import app.liveisland.core.IslandItem
import app.liveisland.core.Kind
import app.liveisland.core.Settings
import app.liveisland.engine.IslandEngine
import app.liveisland.island.Glyph

/**
 * Lets Tasker, Automate, scripts or other apps put things on the island.
 * Off by default; the user turns it on in Automation.
 *
 *   am broadcast -a app.liveisland.SHOW --es id build --es title "Build #42" --es text "Running tests" --ei progress 60 --ei seconds 120
 *   am broadcast -a app.liveisland.DISMISS --es id build
 */
class PluginReceiver(private val ctx: Context, private val settings: () -> Settings) : Source {
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (!settings().features.plugins) return
            val id = "plugin:" + (i.getStringExtra("id") ?: "default").take(40)
            when (i.action) {
                ACTION_DISMISS -> IslandEngine.remove(id)
                ACTION_SHOW -> {
                    val progress = i.getIntExtra("progress", -1)
                    val secs = i.getIntExtra("seconds", 0)
                    val countdown = i.getIntExtra("countdown", 0)
                    val now = System.currentTimeMillis()
                    IslandEngine.post(
                        IslandItem(
                            id, Kind.PLUGIN,
                            title = (i.getStringExtra("title") ?: "").take(60),
                            value = (i.getStringExtra("text") ?: "").take(120),
                            subtitle = (i.getStringExtra("source") ?: "Automation").take(24),
                            progress = progress.takeIf { it in 0..100 }?.div(100f),
                            endsAt = if (countdown > 0) now + countdown * 1000L else null,
                            totalMs = if (countdown > 0) countdown * 1000L else null,
                            expiresAt = if (secs > 0) now + secs * 1000L else null,
                            glyph = Glyph.SPARK,
                        )
                    )
                }
            }
        }
    }

    override fun start() {
        val f = IntentFilter().apply { addAction(ACTION_SHOW); addAction(ACTION_DISMISS) }
        ContextCompat.registerReceiver(ctx, receiver, f, ContextCompat.RECEIVER_EXPORTED)
    }

    override fun stop() = runCatching { ctx.unregisterReceiver(receiver) }.let { }

    companion object {
        const val ACTION_SHOW = "app.liveisland.SHOW"
        const val ACTION_DISMISS = "app.liveisland.DISMISS"
    }
}
