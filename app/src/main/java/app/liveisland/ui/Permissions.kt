package app.liveisland.ui

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.liveisland.service.IslandAccessibilityService
import app.liveisland.service.IslandNotificationListener

/** Every permission, what it's for, and the shortest path to granting it. */
object Permissions {
    fun overlay(ctx: Context): Boolean {
        val flat = ComponentName(ctx, IslandAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.split(':').any { it.equals(flat, true) || it.equals(ComponentName(ctx, IslandAccessibilityService::class.java).flattenToShortString(), true) }
    }

    fun notifications(ctx: Context) = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

    val runtime: Array<String> get() = buildList {
        add(Manifest.permission.READ_CALENDAR)
        add(Manifest.permission.ANSWER_PHONE_CALLS)
        if (Build.VERSION.SDK_INT >= 31) add(Manifest.permission.BLUETOOTH_CONNECT)
    }.toTypedArray()

    fun runtimeGranted(ctx: Context) = runtime.all { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }

    fun dnd(ctx: Context) = ctx.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

    /** Jumps straight to our switch where Android allows it, otherwise to the list with us highlighted. */
    fun openOverlay(ctx: Context) {
        val cn = ComponentName(ctx, IslandAccessibilityService::class.java).flattenToString()
        val details = Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS").putExtra(Intent.EXTRA_COMPONENT_NAME, cn)
        val list = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            putExtra(":settings:fragment_args_key", cn)
            putExtra(":settings:show_fragment_args", Bundle().apply { putString(":settings:fragment_args_key", cn) })
        }
        start(ctx, details) || start(ctx, list)
    }

    fun openNotifications(ctx: Context) {
        val cn = ComponentName(ctx, IslandNotificationListener::class.java)
        val detail = if (Build.VERSION.SDK_INT >= 30) Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, cn.flattenToString()) else null
        (detail != null && start(ctx, detail)) || start(ctx, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    fun openDnd(ctx: Context) = start(ctx, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))

    /** Android 13+ blocks accessibility for sideloaded apps until "Allow restricted settings" is tapped here. */
    fun openAppInfo(ctx: Context) = start(ctx, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))

    private fun start(ctx: Context, i: Intent): Boolean = runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }.getOrDefault(false)

    /** Proof for the privacy screen: we read our own manifest. */
    fun hasInternet(ctx: Context): Boolean = runCatching {
        val info = ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_PERMISSIONS)
        info.requestedPermissions?.contains(Manifest.permission.INTERNET) == true
    }.getOrDefault(false)
}
