package app.liveisland.service

import android.content.Context
import android.graphics.Rect
import android.view.DisplayCutout
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
    /**
     * Reads the display cutout. Falls back to the middle of the status bar when there is none (or under-display camera).
     * Pass [cutout] only from a full-screen window (the app's activity). The overlay must pass null:
     * a small window's insets are relative to the window itself, which made the island chase its own tail.
     */
    fun detect(ctx: Context, cutout: DisplayCutout?): LensSpot {
        val display = ctx.getSystemService(android.hardware.display.DisplayManager::class.java).getDisplay(android.view.Display.DEFAULT_DISPLAY)
        val size = android.graphics.Point().also { @Suppress("DEPRECATION") display.getRealSize(it) }
        val w = size.x
        val c: DisplayCutout? = cutout ?: display.cutout
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
