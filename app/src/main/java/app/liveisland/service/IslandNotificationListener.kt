package app.liveisland.service

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import app.liveisland.core.SettingsStore
import app.liveisland.sources.MediaTracker
import app.liveisland.sources.NotificationRouter

/** Reads notifications and media sessions on-device. Nothing leaves the phone. */
class IslandNotificationListener : NotificationListenerService() {
    private lateinit var router: NotificationRouter
    private var media: MediaTracker? = null

    override fun onCreate() {
        super.onCreate()
        val store = SettingsStore.get(this)
        router = NotificationRouter(this) { store.settings.value }
        media = MediaTracker(this, ComponentName(this, IslandNotificationListener::class.java)) { store.settings.value }
    }

    override fun onListenerConnected() {
        connected = true
        media?.start()
        runCatching { activeNotifications?.forEach { router.posted(it) } }
    }

    override fun onListenerDisconnected() {
        connected = false
        media?.stop()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = router.posted(sbn)
    override fun onNotificationRemoved(sbn: StatusBarNotification) = router.removed(sbn)

    companion object {
        @Volatile var connected = false
    }
}
