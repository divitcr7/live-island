package app.liveisland.shelf

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import app.liveisland.engine.IslandEngine
import app.liveisland.engine.Mode

/** Invisible share target: "Share → Live Island" drops the thing on the shelf and closes. */
class ShareToShelfActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val i = intent
        when (i?.action) {
            Intent.ACTION_SEND -> {
                val uri = stream(i)
                if (uri != null) ShelfStore.addUri(this, uri, i.type)
                else i.getStringExtra(Intent.EXTRA_TEXT)?.let { ShelfStore.addText(this, it) }
            }
            Intent.ACTION_SEND_MULTIPLE -> streams(i).forEach { ShelfStore.addUri(this, it, i.type) }
        }
        IslandEngine.setMode(Mode.SHELF)
        finish()
    }

    private fun stream(i: Intent): Uri? = if (Build.VERSION.SDK_INT >= 33) i.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    else @Suppress("DEPRECATION") i.getParcelableExtra(Intent.EXTRA_STREAM)

    private fun streams(i: Intent): List<Uri> = (if (Build.VERSION.SDK_INT >= 33) i.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
    else @Suppress("DEPRECATION") i.getParcelableArrayListExtra(Intent.EXTRA_STREAM)) ?: emptyList()
}
