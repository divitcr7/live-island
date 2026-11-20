package app.liveisland.sources

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.palette.graphics.Palette
import app.liveisland.core.IslandAction
import app.liveisland.core.IslandItem
import app.liveisland.core.Kind
import app.liveisland.core.Settings
import app.liveisland.engine.IslandEngine
import app.liveisland.island.Glyph

/** Follows whichever app is playing media, through the system media sessions. Works with any player. */
class MediaTracker(private val ctx: Context, private val listener: ComponentName, private val settings: () -> Settings) {
    private val msm = ctx.getSystemService(MediaSessionManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var controller: MediaController? = null
    private var artKey: Any? = null
    private var art: Pair<androidx.compose.ui.graphics.ImageBitmap, Color?>? = null

    private val sessions = MediaSessionManager.OnActiveSessionsChangedListener { pick(it ?: emptyList()) }

    private val cb = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onSessionDestroyed() { controller = null; IslandEngine.remove(ID) }
    }

    fun start() {
        runCatching {
            msm.addOnActiveSessionsChangedListener(sessions, listener, main)
            pick(msm.getActiveSessions(listener))
        }
    }

    fun stop() {
        runCatching { msm.removeOnActiveSessionsChangedListener(sessions) }
        controller?.unregisterCallback(cb)
        controller = null
        IslandEngine.remove(ID)
    }

    private fun pick(list: List<MediaController>) {
        val best = list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list.firstOrNull()
        if (best?.sessionToken == controller?.sessionToken) { publish(); return }
        controller?.unregisterCallback(cb)
        controller = best
        best?.registerCallback(cb, main)
        publish()
    }

    private fun publish() {
        val c = controller
        val md = c?.metadata
        val st = c?.playbackState
        if (c == null || md == null || st == null || !settings().features.media ||
            st.state in setOf(PlaybackState.STATE_NONE, PlaybackState.STATE_STOPPED, PlaybackState.STATE_ERROR) ||
            c.packageName in settings().blockedApps
        ) { IslandEngine.remove(ID); return }

        val playing = st.state == PlaybackState.STATE_PLAYING || st.state == PlaybackState.STATE_BUFFERING
        val bmp: Bitmap? = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: md.getBitmap(MediaMetadata.METADATA_KEY_ART)
        if (bmp !== artKey) {
            artKey = bmp
            art = bmp?.let { b ->
                val small = Bitmap.createScaledBitmap(b, 160, 160 * b.height / b.width.coerceAtLeast(1), true)
                val sw = runCatching { Palette.from(small).generate().let { it.vibrantSwatch ?: it.lightVibrantSwatch ?: it.dominantSwatch } }.getOrNull()
                small.asImageBitmap() to sw?.rgb?.let { Color(it) }
            }
        }
        // translate the elapsed-realtime position into wall clock so the UI can extrapolate
        val wallNow = System.currentTimeMillis()
        val pos = st.position + if (playing && st.lastPositionUpdateTime > 0) ((SystemClock.elapsedRealtime() - st.lastPositionUpdateTime) * st.playbackSpeed).toLong() else 0
        val t = c.transportControls
        IslandEngine.post(
            IslandItem(
                id = ID, kind = Kind.MEDIA,
                title = md.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "",
                value = md.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: "",
                subtitle = appLabel(ctx, c.packageName) ?: "",
                art = art?.first, artAccent = art?.second, packageName = c.packageName,
                playing = playing, positionMs = pos, positionAt = wallNow,
                durationMs = md.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0), speed = if (playing) st.playbackSpeed.coerceAtLeast(0.1f) else 0f,
                priorityBoost = if (playing) 0 else -40,
                actions = listOf(
                    IslandAction("prev", "Previous", Glyph.PREV) { t.skipToPrevious() },
                    IslandAction("play", if (playing) "Pause" else "Play", if (playing) Glyph.PAUSE else Glyph.PLAY, primary = true) { if (playing) t.pause() else t.play() },
                    IslandAction("next", "Next", Glyph.NEXT) { t.skipToNext() },
                ),
                onOpen = { c.sessionActivity?.let { runCatching { it.send() } } },
            )
        )
    }

    companion object { const val ID = "media" }
}
