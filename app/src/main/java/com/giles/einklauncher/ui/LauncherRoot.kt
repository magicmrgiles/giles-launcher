package com.giles.einklauncher.ui

import android.Manifest
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.giles.einklauncher.DefaultLauncherHelper
import com.giles.einklauncher.data.settings.ClockStyle
import com.giles.einklauncher.notifications.NotificationAccess
import com.giles.einklauncher.ui.components.ConfirmDialog
import com.giles.einklauncher.ui.components.GestureNavPill
import com.giles.einklauncher.ui.home.HomeScreen
import com.giles.einklauncher.ui.home.WidgetExpansion
import com.giles.einklauncher.ui.home.AppPickerSheet
import com.giles.einklauncher.ui.notes.NotesScreen
import com.giles.einklauncher.ui.settings.SettingsSheet
import com.giles.einklauncher.ui.theme.einkColors
import com.giles.einklauncher.ui.util.rememberResumeKey
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private enum class Rationale { CALENDAR, LOCATION }

@Composable
fun LauncherRoot(
    viewModel: LauncherViewModel,
    homeReset: SharedFlow<Unit>,
    roleResultSignal: StateFlow<Int>,
    onRequestDefaultLauncher: () -> Unit,
) {
    val context = LocalContext.current
    val colors = einkColors
    val scope = rememberCoroutineScope()

    val settings by viewModel.settings.collectAsState()
    val slots by viewModel.pinnedSlots.collectAsState()
    val alarm by viewModel.alarm.collectAsState()
    val calendar by viewModel.calendar.collectAsState()
    val weather by viewModel.weather.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val noteLines by viewModel.noteLines.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 2 })

    var editMode by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var pickerSlot by remember { mutableStateOf<Int?>(null) }
    var expansion by remember { mutableStateOf(WidgetExpansion.NONE) }
    var rationale by remember { mutableStateOf<Rationale?>(null) }

    // State that can change while backgrounded.
    val resumeKey = rememberResumeKey()
    val roleResult by roleResultSignal.collectAsState()
    val isDefaultLauncher = remember(resumeKey, roleResult) { DefaultLauncherHelper.isDefault(context) }
    val notificationAccess = remember(resumeKey) { NotificationAccess.isEnabled(context) }

    val deviceIs24 = remember(resumeKey) { DateFormat.is24HourFormat(context) }
    val is24Hour = when (settings.clockStyle) {
        ClockStyle.TWENTY_FOUR_HOUR -> true
        ClockStyle.TWELVE_HOUR -> false
        null -> deviceIs24
    }

    // Permission launchers.
    val calendarPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshCalendar() }

    val locationPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshWeather() }

    // Reset to home + close overlays when HOME is pressed.
    LaunchedEffect(Unit) {
        homeReset.collect {
            showSettings = false
            pickerSlot = null
            editMode = false
            expansion = WidgetExpansion.NONE
            pagerState.animateScrollToPage(0)
        }
    }

    // Refresh the app list whenever the picker opens (catches newly installed apps).
    LaunchedEffect(pickerSlot) {
        if (pickerSlot != null) viewModel.refreshInstalledApps()
    }

    BackHandler(enabled = true) {
        when {
            showSettings -> showSettings = false
            pickerSlot != null -> pickerSlot = null
            editMode -> editMode = false
            expansion != WidgetExpansion.NONE -> expansion = WidgetExpansion.NONE
            pagerState.currentPage != 0 -> scope.launch { pagerState.animateScrollToPage(0) }
            else -> { /* On home: do nothing — a launcher must not exit. */ }
        }
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> HomeScreen(
                    is24Hour = is24Hour,
                    alarm = alarm,
                    calendar = calendar,
                    weather = weather,
                    expansion = expansion,
                    slots = slots,
                    editMode = editMode,
                    onToggleEvent = {
                        expansion = if (expansion == WidgetExpansion.EVENT) WidgetExpansion.NONE else WidgetExpansion.EVENT
                    },
                    onToggleWeather = {
                        expansion = if (expansion == WidgetExpansion.WEATHER) WidgetExpansion.NONE else WidgetExpansion.WEATHER
                    },
                    onRequestCalendarPermission = { rationale = Rationale.CALENDAR },
                    onRequestLocation = { rationale = Rationale.LOCATION },
                    onRetryWeather = { viewModel.refreshWeather() },
                    onLaunch = viewModel::launchApp,
                    onEnterEdit = { editMode = true },
                    onExitEdit = { editMode = false },
                    onRemove = { index -> viewModel.removeApp(index) },
                    onEmptyTap = { index -> pickerSlot = index },
                    onOpenSettings = { showSettings = true },
                )

                1 -> NotesScreen(
                    lines = noteLines,
                    onAppend = viewModel::appendNoteLine,
                    onToggleCheckbox = viewModel::toggleCheckbox,
                    onClear = viewModel::clearNote,
                )
            }
        }

        GestureNavPill(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )

        pickerSlot?.let { slotIndex ->
            val pinnedRefs = slots.filterIsInstance<PinnedSlot.Filled>().map { it.ref }.toSet()
            val available = installedApps.filter { it.ref !in pinnedRefs }
            AppPickerSheet(
                apps = available,
                onPick = { app ->
                    viewModel.assignApp(slotIndex, app.ref)
                    pickerSlot = null
                    editMode = false
                },
                onDismiss = { pickerSlot = null },
            )
        }

        if (showSettings) {
            SettingsSheet(
                settings = settings,
                deviceIs24Hour = deviceIs24,
                isDefaultLauncher = isDefaultLauncher,
                notificationAccessGranted = notificationAccess,
                onThemeMode = viewModel::setThemeMode,
                onClockStyle = viewModel::setClockStyle,
                onWeatherLocation = viewModel::setWeatherLocation,
                onAppCount = viewModel::setAppCount,
                onNotifFilter = viewModel::setNotificationFilter,
                onOpenNotificationAccess = { NotificationAccess.openSettings(context) },
                onRequestDefaultLauncher = onRequestDefaultLauncher,
                onDismiss = { showSettings = false },
            )
        }

        when (rationale) {
            Rationale.CALENDAR -> ConfirmDialog(
                title = "Show your calendar",
                message = "Eink Launcher needs calendar access to show your next event and today's schedule. Nothing leaves your device.",
                confirmLabel = "Continue",
                dismissLabel = "Not now",
                onConfirm = {
                    rationale = null
                    calendarPermission.launch(Manifest.permission.READ_CALENDAR)
                },
                onDismiss = { rationale = null },
            )

            Rationale.LOCATION -> ConfirmDialog(
                title = "Use your location",
                message = "Eink Launcher uses your approximate location for local weather. Or set a location manually in Settings.",
                confirmLabel = "Continue",
                dismissLabel = "Not now",
                onConfirm = {
                    rationale = null
                    locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                },
                onDismiss = { rationale = null },
            )

            null -> Unit
        }
    }
}
