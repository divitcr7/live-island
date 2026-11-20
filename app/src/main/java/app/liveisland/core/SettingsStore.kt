package app.liveisland.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private val Context.store by preferencesDataStore("live_island")
private val KEY = stringPreferencesKey("settings_v1")

/** Single source of truth for settings. Shared by the app UI and the overlay service. */
class SettingsStore private constructor(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    private fun decode(raw: String?): Settings =
        raw?.let { runCatching { json.decodeFromString(Settings.serializer(), it) }.getOrNull() } ?: Settings()

    val settings: StateFlow<Settings> = context.store.data
        .map { decode(it[KEY]) }
        .stateIn(scope, SharingStarted.Eagerly, runBlocking { decode(context.store.data.first()[KEY]) })

    fun update(change: (Settings) -> Settings) {
        scope.launch {
            context.store.edit { prefs ->
                val next = change(decode(prefs[KEY]))
                prefs[KEY] = json.encodeToString(Settings.serializer(), next)
            }
        }
    }

    companion object {
        @Volatile private var instance: SettingsStore? = null
        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) { instance ?: SettingsStore(context.applicationContext).also { instance = it } }
    }
}
