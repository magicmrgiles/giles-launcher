package com.giles.einklauncher.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.giles.einklauncher.data.launcherDataStore
import com.giles.einklauncher.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Reads and writes [LauncherSettings] through the shared DataStore. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val CLOCK = stringPreferencesKey("clock_style") // "12", "24", or absent = device default
        val WEATHER_LOCATION = stringPreferencesKey("weather_location")
        val APP_COUNT = intPreferencesKey("app_count")
        val NOTIF_FILTER = booleanPreferencesKey("notif_filter")
    }

    val settings: Flow<LauncherSettings> = context.launcherDataStore.data.map { p ->
        LauncherSettings(
            themeMode = when (p[Keys.THEME]) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            },
            clockStyle = when (p[Keys.CLOCK]) {
                "12" -> ClockStyle.TWELVE_HOUR
                "24" -> ClockStyle.TWENTY_FOUR_HOUR
                else -> null
            },
            weatherLocation = p[Keys.WEATHER_LOCATION].orEmpty(),
            appCount = p[Keys.APP_COUNT] ?: 8,
            notificationFilterEnabled = p[Keys.NOTIF_FILTER] ?: false,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME] = mode.name }

    suspend fun setClockStyle(style: ClockStyle?) = edit {
        if (style == null) it.remove(Keys.CLOCK)
        else it[Keys.CLOCK] = if (style == ClockStyle.TWELVE_HOUR) "12" else "24"
    }

    suspend fun setWeatherLocation(location: String) = edit {
        it[Keys.WEATHER_LOCATION] = location.trim()
    }

    suspend fun setAppCount(count: Int) = edit { it[Keys.APP_COUNT] = count }

    suspend fun setNotificationFilter(enabled: Boolean) = edit { it[Keys.NOTIF_FILTER] = enabled }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.launcherDataStore.edit(block)
    }
}
