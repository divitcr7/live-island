package app.liveisland.service

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import app.liveisland.core.SettingsStore
import app.liveisland.sources.MediaTracker
import app.liveisland.sources.NotificationRouter
import kotlinx.coroutines.launch

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

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate)
    private var mediaOn = false

    override fun onListenerConnected() {
        connected = true
        // Follow media sessions only while Music is switched on.
        val store = SettingsStore.get(this)
        scope.launch {
            store.settings.collect { s ->
                val want = s.enabled && s.features.media
                if (want && !mediaOn) { media?.start(); mediaOn = true }
                else if (!want && mediaOn) { media?.stop(); mediaOn = false }
            }
        }
        runCatching { activeNotifications?.forEach { router.posted(it) } }
    }

    override fun onListenerDisconnected() {
        connected = false
        scope.coroutineContext[kotlinx.coroutines.Job]?.children?.forEach { it.cancel() }
        if (mediaOn) { media?.stop(); mediaOn = false }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = router.posted(sbn)
    override fun onNotificationRemoved(sbn: StatusBarNotification) = router.removed(sbn)

    companion object {
        @Volatile var connected = false
    }
}
