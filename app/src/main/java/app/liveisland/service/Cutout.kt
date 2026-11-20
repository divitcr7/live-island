package app.liveisland.service

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.DisplayCutout
import android.view.WindowManager
import app.liveisland.core.Anchor
import app.liveisland.island.LensSide

/** Where the front camera is, in screen pixels. */
data class LensSpot(val cx: Int, val cy: Int, val screenW: Int, val detected: Boolean) {
    fun side(anchor: Anchor): LensSide = when (anchor) {
        Anchor.LEFT -> LensSide.START
        Anchor.RIGHT -> LensSide.END
        Anchor.CENTER -> LensSide.CENTER
        Anchor.AUTO -> when {
            !detected -> LensSide.CENTER
            cx < screenW * 0.33f -> LensSide.START
            cx > screenW * 0.67f -> LensSide.END
            else -> LensSide.CENTER
        }
    }
}

object Cutout {
    /** Reads the display cutout. Falls back to the middle of the status bar when there is none (or under-display camera). */
    fun detect(ctx: Context, cutout: DisplayCutout?): LensSpot {
        val wm = ctx.getSystemService(WindowManager::class.java)
        val w: Int
        val c: DisplayCutout?
        if (Build.VERSION.SDK_INT >= 30) {
            val m = wm.currentWindowMetrics
            w = m.bounds.width()
            c = cutout ?: m.windowInsets.displayCutout
        } else {
            w = ctx.resources.displayMetrics.widthPixels
            c = cutout
        }
        val top: Rect? = c?.boundingRects?.filter { it.top <= 4 && it.width() < w * 0.6f }?.minByOrNull { it.top }
        val sbH = statusBarHeight(ctx)
        return if (top != null && !top.isEmpty) LensSpot(top.centerX(), top.centerY().coerceAtLeast(sbH / 2), w, true)
        else LensSpot(w / 2, sbH / 2, w, false)
    }

    fun statusBarHeight(ctx: Context): Int {
        @Suppress("DiscouragedApi", "InternalInsetResource")
        val id = ctx.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) ctx.resources.getDimensionPixelSize(id) else (24 * ctx.resources.displayMetrics.density).toInt()
    }
}
