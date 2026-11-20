package app.liveisland.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.liveisland.core.SettingsStore
import app.liveisland.ui.MainActivity
import app.liveisland.ui.Permissions

/** Quick Settings tile: turn the island on or off from the notification shade. */
class IslandTileService : TileService() {
    override fun onStartListening() = refresh()

    override fun onClick() {
        if (!Permissions.overlay(this)) {
            // Not set up yet: open the app where the Turn on button is.
            val i = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= 34) startActivityAndCollapse(PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE))
            else @Suppress("DEPRECATION") startActivityAndCollapse(i)
            return
        }
        val store = SettingsStore.get(this)
        val next = !store.settings.value.enabled
        store.update { it.copy(enabled = next) }
        render(next)
    }

    private fun refresh() = render(Permissions.overlay(this) && SettingsStore.get(this).settings.value.enabled)

    private fun render(on: Boolean) {
        val t = qsTile ?: return
        t.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= 29) t.subtitle = if (!Permissions.overlay(this)) "Set up" else if (on) "On" else "Off"
        t.updateTile()
    }
}
