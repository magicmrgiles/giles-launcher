package com.giles.einklauncher.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.weight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.giles.einklauncher.data.apps.PinnedAppRef
import com.giles.einklauncher.ui.AlarmUiState
import com.giles.einklauncher.ui.CalendarUiState
import com.giles.einklauncher.ui.PinnedSlot
import com.giles.einklauncher.ui.WeatherUiState

/**
 * Screen 1. The widget sits at the top; the pinned-app grid fills the rest, bottom-aligned.
 */
@Composable
fun HomeScreen(
    is24Hour: Boolean,
    alarm: AlarmUiState,
    calendar: CalendarUiState,
    weather: WeatherUiState,
    expansion: WidgetExpansion,
    slots: List<PinnedSlot>,
    editMode: Boolean,
    onToggleEvent: () -> Unit,
    onToggleWeather: () -> Unit,
    onRequestCalendarPermission: () -> Unit,
    onRequestLocation: () -> Unit,
    onRetryWeather: () -> Unit,
    onLaunch: (PinnedAppRef) -> Unit,
    onEnterEdit: () -> Unit,
    onExitEdit: () -> Unit,
    onRemove: (Int) -> Unit,
    onEmptyTap: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Widget(
            is24Hour = is24Hour,
            alarm = alarm,
            calendar = calendar,
            weather = weather,
            expansion = expansion,
            onToggleEvent = onToggleEvent,
            onToggleWeather = onToggleWeather,
            onRequestCalendarPermission = onRequestCalendarPermission,
            onRequestLocation = onRequestLocation,
            onRetryWeather = onRetryWeather,
            onLongPress = onOpenSettings,
        )
        Box(Modifier.weight(1f)) {
            AppGrid(
                slots = slots,
                editMode = editMode,
                onLaunch = onLaunch,
                onEnterEdit = onEnterEdit,
                onExitEdit = onExitEdit,
                onRemove = onRemove,
                onEmptyTap = onEmptyTap,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}
