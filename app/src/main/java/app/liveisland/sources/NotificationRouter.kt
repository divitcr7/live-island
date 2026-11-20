package app.liveisland.sources

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.telecom.TelecomManager
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import app.liveisland.core.IslandAction
import app.liveisland.core.IslandItem
import app.liveisland.core.Kind
import app.liveisland.core.PeekMode
import app.liveisland.core.Settings
import app.liveisland.engine.IslandEngine
import app.liveisland.engine.Otp
import app.liveisland.island.Glyph

/** Turns notifications into island activities. Nothing is stored or sent anywhere. */
class NotificationRouter(private val ctx: Context, private val settings: () -> Settings) {

    private val navApps = setOf("com.google.android.apps.maps", "com.waze", "com.here.app.maps", "net.osmand", "net.osmand.plus", "com.mapbox.navigation", "app.organicmaps")
    private val seenOtp = mutableSetOf<String>()

    fun posted(sbn: StatusBarNotification) {
        val s = settings()
        val n = sbn.notification
        val pkg = sbn.packageName
        if (pkg == ctx.packageName || pkg in s.blockedApps) return
        if ((n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        val x = n.extras
        val title = x.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (x.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: x.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        val sub = x.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
        val template = x.getString(Notification.EXTRA_TEMPLATE).orEmpty()
        val key = "n:${sbn.key}"
        val ongoing = (n.flags and Notification.FLAG_ONGOING_EVENT) != 0

        // media is handled by MediaTracker via sessions
        if (n.category == Notification.CATEGORY_TRANSPORT || template.endsWith("MediaStyle")) return

        // 1. calls
        if (n.category == Notification.CATEGORY_CALL || template.endsWith("CallStyle")) {
            if (s.features.calls) call(sbn, title, x, ongoing)
            return
        }

        // 2. one-time codes beat everything else
        if (s.features.otp && !ongoing) {
            val code = Otp.find("$title $text")
            if (code != null && seenOtp.add("$pkg:$code")) {
                otp(code, appLabel(ctx, pkg) ?: pkg)
                return
            }
        }

        // 3. timers / stopwatches with a chronometer
        val chrono = x.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER)
        if (s.features.timers && chrono && (n.category in setOf(Notification.CATEGORY_ALARM, Notification.CATEGORY_STOPWATCH) || pkg.contains("clock", true))) {
            val down = x.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN)
            IslandEngine.post(
                IslandItem(
                    key, Kind.TIMER, title = title.ifEmpty { if (down) "Timer" else "Stopwatch" },
                    glyph = if (down) Glyph.HOURGLASS else Glyph.STOPWATCH, packageName = pkg,
                    endsAt = if (down) n.`when` else null, startedAt = if (down) null else n.`when`,
                    totalMs = if (down) (n.`when` - sbn.postTime).coerceAtLeast(1) else null,
                    actions = actions(n, pkg), onOpen = open(n),
                )
            )
            return
        }

        // 4. navigation
        if (s.features.navigation && (n.category == Notification.CATEGORY_NAVIGATION || pkg in navApps) && ongoing) {
            val icon = n.getLargeIcon()?.let { loadIcon(it) }
            IslandEngine.post(
                IslandItem(
                    key, Kind.NAVIGATION, title = text.ifEmpty { title }, value = title.takeIf { text.isNotEmpty() } ?: "",
                    subtitle = sub, art = icon, packageName = pkg, onOpen = open(n),
                )
            )
            return
        }

        // 5. live updates: anything ongoing with progress, or Android 16 promoted ongoing notifications
        val max = x.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val indeterminate = x.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE)
        val promoted = (n.flags and FLAG_PROMOTED_ONGOING) != 0
        if (s.features.liveUpdates && (max > 0 || indeterminate || promoted) && (ongoing || promoted)) {
            val p = if (max > 0) x.getInt(Notification.EXTRA_PROGRESS, 0).toFloat() / max else null
            IslandEngine.post(
                IslandItem(
                    key, Kind.LIVE, title = title.ifEmpty { text }, value = if (title.isNotEmpty()) text else "",
                    subtitle = appLabel(ctx, pkg) ?: "", progress = p, packageName = pkg,
                    glyph = if (n.category == Notification.CATEGORY_PROGRESS) Glyph.DOWN else Glyph.BOX,
                    onOpen = open(n),
                )
            )
            return
        }

        // 6. message peeks
        if (ongoing || s.features.peek == PeekMode.OFF) return
        val isMessage = n.category == Notification.CATEGORY_MESSAGE || template.endsWith("MessagingStyle")
        if (s.features.peek == PeekMode.MESSAGES && !isMessage) return
        if (title.isEmpty() && text.isEmpty()) return
        val reply = n.actions?.firstOrNull { a -> a.remoteInputs?.any { it.allowFreeFormInput } == true }
        IslandEngine.post(
            IslandItem(
                "peek", Kind.MESSAGE, title = title, value = text, subtitle = appLabel(ctx, pkg) ?: "",
                packageName = pkg, expiresAt = System.currentTimeMillis() + 6_000,
                replies = if (reply != null) listOf("👍", "On my way") else emptyList(),
                onReply = reply?.let { a -> { msg: String -> sendReply(a, msg); IslandEngine.remove("peek") } },
                onOpen = { open(n)?.invoke(); IslandEngine.remove("peek") },
            )
        )
    }

    fun removed(sbn: StatusBarNotification) {
        val key = "n:${sbn.key}"
        IslandEngine.remove(key)
        if (sbn.notification.category == Notification.CATEGORY_CALL) IslandEngine.remove(CALL_ID)
    }

    // ---------------- helpers ----------------

    private fun call(sbn: StatusBarNotification, title: String, x: Bundle, ongoing: Boolean) {
        val n = sbn.notification
        val callType = x.getInt("android.callType", 0) // 1 incoming, 2 ongoing, 3 screening
        val incoming = callType == 1 || (callType == 0 && n.fullScreenIntent != null && !x.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        val acts = n.actions.orEmpty()
        fun find(vararg words: String) = acts.firstOrNull { a -> words.any { a.title?.toString()?.contains(it, true) == true } }
        val decline = find("decline", "reject", "dismiss", "hang", "end")
        val answer = find("answer", "accept", "pick")
        val tm = ctx.getSystemService(TelecomManager::class.java)
        if (incoming) {
            IslandEngine.post(
                IslandItem(
                    CALL_ID, Kind.CALL, title = title.ifEmpty { "Unknown" }, subtitle = "mobile", glyph = Glyph.PHONE, packageName = sbn.packageName,
                    actions = listOf(
                        IslandAction("decline", "Decline", Glyph.PHONE) { decline?.actionIntent?.let(::fire) ?: runCatching { @Suppress("DEPRECATION", "MissingPermission") tm.endCall() } },
                        IslandAction("answer", "Answer", Glyph.PHONE, primary = true) { answer?.actionIntent?.let(::fire) ?: runCatching { @Suppress("MissingPermission") tm.acceptRingingCall() } },
                    ),
                    onOpen = open(n),
                )
            )
        } else if (ongoing) {
            val hang = find("hang", "end", "decline")
            IslandEngine.post(
                IslandItem(
                    CALL_ID, Kind.CALL, title = title.ifEmpty { "Call" }, startedAt = if (n.`when` > 0) n.`when` else sbn.postTime,
                    glyph = Glyph.PHONE, packageName = sbn.packageName,
                    actions = listOfNotNull(
                        find("mute")?.let { a -> IslandAction("mute", "Mute", Glyph.MIC) { a.actionIntent?.let(::fire) } },
                        find("speaker")?.let { a -> IslandAction("speaker", "Speaker", Glyph.SPEED) { a.actionIntent?.let(::fire) } },
                        IslandAction("end", "End", Glyph.CLOSE, primary = true) { hang?.actionIntent?.let(::fire) ?: runCatching { @Suppress("DEPRECATION", "MissingPermission") tm.endCall() } },
                    ),
                    onOpen = open(n),
                )
            )
        }
    }

    private fun otp(code: String, from: String) {
        IslandEngine.post(
            IslandItem(
                OTP_ID, Kind.OTP, value = code, subtitle = from, glyph = Glyph.KEY,
                expiresAt = System.currentTimeMillis() + 30_000,
                actions = listOf(IslandAction("copy", "Copy", Glyph.COPY, primary = true) {
                    ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("code", code))
                    IslandEngine.remove(OTP_ID)
                }),
            )
        )
        IslandEngine.haptic?.invoke(true)
    }

    private fun actions(n: Notification, pkg: String): List<IslandAction> =
        n.actions.orEmpty().take(2).map { a -> IslandAction(a.title.toString(), a.title.toString(), null) { a.actionIntent?.let(::fire) } }

    private fun open(n: Notification): (() -> Unit)? = n.contentIntent?.let { pi -> { fire(pi) } }

    private fun fire(pi: PendingIntent) {
        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                val opts = android.app.ActivityOptions.makeBasic().setPendingIntentBackgroundActivityStartMode(android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                pi.send(ctx, 0, null, null, null, null, opts.toBundle())
            } else pi.send()
        }
    }

    private fun sendReply(a: Notification.Action, msg: String) {
        val inputs = a.remoteInputs ?: return
        val intent = Intent()
        val bundle = Bundle().apply { inputs.forEach { putCharSequence(it.resultKey, msg) } }
        RemoteInput.addResultsToIntent(inputs, intent, bundle)
        runCatching { a.actionIntent.send(ctx, 0, intent) }
    }

    private fun loadIcon(i: Icon) = runCatching { i.loadDrawable(ctx)?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull()

    companion object {
        const val CALL_ID = "call"
        const val OTP_ID = "otp"
        /** Notification.FLAG_PROMOTED_ONGOING (Android 16). */
        const val FLAG_PROMOTED_ONGOING = 0x00040000
    }
}
