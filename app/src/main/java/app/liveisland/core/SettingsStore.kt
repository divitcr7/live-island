package app.liveisland.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private val Context.store by preferencesDataStore("live_island")
private val KEY = stringPreferencesKey("settings_v1")

/** Single source of truth for settings. Shared by the app UI and the overlay service (same process). */
@OptIn(kotlinx.coroutines.FlowPreview::class)
class SettingsStore private constructor(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    private fun decode(raw: String?): Settings =
        raw?.let { runCatching { json.decodeFromString(Settings.serializer(), it) }.getOrNull() } ?: Settings()

    private val mem = MutableStateFlow(runBlocking { decode(context.store.data.first()[KEY]) })

    /** Changes apply instantly in memory; the disk write waits until changes stop (drags, sliders). */
    val settings: StateFlow<Settings> = mem.asStateFlow()

    init {
        scope.launch {
            mem.drop(1).debounce(400).collect { next ->
                context.store.edit { it[KEY] = json.encodeToString(Settings.serializer(), next) }
            }
        }
    }

    fun update(change: (Settings) -> Settings) = mem.update(change)

    companion object {
        @Volatile private var instance: SettingsStore? = null
        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) { instance ?: SettingsStore(context.applicationContext).also { instance = it } }
    }
}
