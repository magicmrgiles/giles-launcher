package com.giles.einklauncher.ui

import com.giles.einklauncher.data.apps.PinnedAppRef
import com.giles.einklauncher.data.widget.CalendarEvent
import com.giles.einklauncher.data.widget.weather.Weather

/** One rendered grid position. */
sealed interface PinnedSlot {
    val index: Int

    data class Empty(override val index: Int) : PinnedSlot
    data class Filled(
        override val index: Int,
        val ref: PinnedAppRef,
        val label: String,
        val launchable: Boolean,
    ) : PinnedSlot
}

/** Next-alarm widget state. */
data class AlarmUiState(val triggerTimeMillis: Long? = null)

/** Calendar widget state; distinguishes "not granted" from "granted but empty". */
sealed interface CalendarUiState {
    data object PermissionRequired : CalendarUiState
    data object Loading : CalendarUiState
    data class Ready(val next: CalendarEvent?, val restOfDay: List<CalendarEvent>) : CalendarUiState
}

/** Weather widget state. */
sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data class Ready(val weather: Weather) : WeatherUiState
    data object NeedsLocationPermission : WeatherUiState
    data object Offline : WeatherUiState
    data object Error : WeatherUiState
}
