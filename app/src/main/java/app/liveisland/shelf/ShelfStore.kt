package app.liveisland.shelf

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class ShelfEntry(
    val id: String,
    /** "text" or a mime type like "image/png". */
    val type: String,
    val text: String? = null,
    val fileName: String? = null,
    val label: String,
    val addedAt: Long,
) {
    val isText get() = type == "text"
    val isImage get() = type.startsWith("image/")
}

/** Things the user dropped on the island. Files live in app-private storage and never leave the phone unless the user shares them. */
object ShelfStore {
    private val _entries = MutableStateFlow<List<ShelfEntry>>(emptyList())
    val entries: StateFlow<List<ShelfEntry>> = _entries.asStateFlow()
    private val json = Json { ignoreUnknownKeys = true }
    private var loaded = false
    const val MAX = 12

    fun dir(context: Context) = File(context.filesDir, "shelf").apply { mkdirs() }
    private fun index(context: Context) = File(dir(context), "index.json")
    fun file(context: Context, e: ShelfEntry) = e.fileName?.let { File(dir(context), it) }

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        _entries.value = runCatching {
            json.decodeFromString(ListSerializer(ShelfEntry.serializer()), index(context).readText())
        }.getOrDefault(emptyList())
    }

    private fun save(context: Context, list: List<ShelfEntry>) {
        val trimmed = list.take(MAX)
        list.drop(MAX).forEach { file(context, it)?.delete() }
        _entries.value = trimmed
        runCatching { index(context).writeText(json.encodeToString(ListSerializer(ShelfEntry.serializer()), trimmed)) }
    }

    fun addText(context: Context, text: String) {
        load(context)
        val label = text.trim().lineSequence().first().take(60)
        save(context, listOf(ShelfEntry(UUID.randomUUID().toString(), "text", text = text, label = label, addedAt = System.currentTimeMillis())) + _entries.value)
    }

    fun addUri(context: Context, uri: Uri, mime: String?): Boolean {
        load(context)
        val cr = context.contentResolver
        val type = mime ?: cr.getType(uri) ?: "application/octet-stream"
        val display = runCatching {
            cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull() ?: "item"
        val id = UUID.randomUUID().toString()
        val ext = display.substringAfterLast('.', "").takeIf { it.length in 1..5 }?.let { ".$it" } ?: ""
        val name = id + ext
        val ok = runCatching {
            cr.openInputStream(uri)?.use { input -> File(dir(context), name).outputStream().use { input.copyTo(it) } } != null
        }.getOrDefault(false)
        if (!ok) return false
        save(context, listOf(ShelfEntry(id, type, fileName = name, label = display, addedAt = System.currentTimeMillis())) + _entries.value)
        return true
    }

    fun remove(context: Context, id: String) {
        val e = _entries.value.firstOrNull { it.id == id } ?: return
        file(context, e)?.delete()
        save(context, _entries.value - e)
    }

    fun clear(context: Context) {
        _entries.value.forEach { file(context, it)?.delete() }
        save(context, emptyList())
    }
}
