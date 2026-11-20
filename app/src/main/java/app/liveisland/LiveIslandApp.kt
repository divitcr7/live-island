package app.liveisland

import android.app.Application
import app.liveisland.core.SettingsStore
import app.liveisland.shelf.ShelfStore

class LiveIslandApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsStore.get(this)
        ShelfStore.load(this)
    }
}
