package com.example.data.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import com.example.data.util.normalizeServerUrl

private val Context.dataStore by preferencesDataStore(name = "pirate_tv_settings")

data class AppConfig(
    val serverUrl: String = "http://sdmx.vip:80",
    val username: String = "",
    val password: String = "",
    val liveExt: String = "m3u8",
    val omdbApiKey: String = ""
)

class AppPreferences(private val context: Context) {

    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD = stringPreferencesKey("password")
        val LIVE_EXT = stringPreferencesKey("live_ext")
        val OMDB_API_KEY = stringPreferencesKey("omdb_api_key")
    }

    val configFlow: Flow<AppConfig> = context.dataStore.data.map { prefs ->
        val rawSrv = prefs[Keys.SERVER_URL] ?: "http://sdmx.vip:80"
        val srv = normalizeServerUrl(rawSrv)

        AppConfig(
            serverUrl = srv,
            username = prefs[Keys.USERNAME] ?: "",
            password = prefs[Keys.PASSWORD] ?: "",
            liveExt = prefs[Keys.LIVE_EXT] ?: "m3u8",
            omdbApiKey = prefs[Keys.OMDB_API_KEY] ?: ""
        )
    }

    suspend fun saveCredentials(username: String, password: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USERNAME] = username.trim()
            prefs[Keys.PASSWORD] = password
        }
    }

    suspend fun clearPassword() {
        context.dataStore.edit { prefs ->
            prefs[Keys.PASSWORD] = ""
        }
    }

    suspend fun saveSettings(serverUrl: String, liveExt: String, omdbApiKey: String) {
        val srv = normalizeServerUrl(serverUrl)

        context.dataStore.edit { prefs ->
            prefs[Keys.SERVER_URL] = srv
            prefs[Keys.LIVE_EXT] = if (liveExt.trim().lowercase() == "m3u8") "m3u8" else "ts"
            prefs[Keys.OMDB_API_KEY] = omdbApiKey.trim()
        }
    }
}
