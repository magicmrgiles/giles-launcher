package com.giles.einklauncher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.giles.einklauncher.data.apps.AppInfo
import com.giles.einklauncher.data.apps.InstalledAppsRepository
import com.giles.einklauncher.data.apps.PinnedAppRef
import com.giles.einklauncher.data.apps.PinnedAppsRepository
import com.giles.einklauncher.data.notes.LineFormat
import com.giles.einklauncher.data.notes.LineType
import com.giles.einklauncher.data.notes.NoteLineEntity
import com.giles.einklauncher.data.notes.NotesRepository
import com.giles.einklauncher.data.settings.ClockStyle
import com.giles.einklauncher.data.settings.LauncherSettings
import com.giles.einklauncher.data.settings.SettingsRepository
import com.giles.einklauncher.data.widget.AlarmRepository
import com.giles.einklauncher.data.widget.CalendarRepository
import com.giles.einklauncher.data.widget.weather.WeatherRepository
import com.giles.einklauncher.data.widget.weather.WeatherResult
import com.giles.einklauncher.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsRepo = SettingsRepository(app)
    private val pinnedRepo = PinnedAppsRepository(app)
    private val installedRepo = InstalledAppsRepository(app)
    private val notesRepo = NotesRepository(app)
    private val alarmRepo = AlarmRepository(app)
    private val calendarRepo = CalendarRepository(app)
    private val weatherRepo = WeatherRepository(app)

    // ---- Settings ----

    val settings: StateFlow<LauncherSettings> =
        settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, LauncherSettings())

    // ---- Installed apps (cached; used for the picker and label resolution) ----

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    // ---- Pinned grid, resolved to display slots ----

    val pinnedSlots: StateFlow<List<PinnedSlot>> =
        combine(pinnedRepo.pinned, settingsRepo.settings, _installedApps) { stored, s, apps ->
            resolveSlots(stored, s.appCount, apps)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---- Notes ----

    val noteLines: StateFlow<List<NoteLineEntity>> =
        notesRepo.lines.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---- Widget: alarm / calendar / weather ----

    private val _alarm = MutableStateFlow(AlarmUiState())
    val alarm: StateFlow<AlarmUiState> = _alarm.asStateFlow()

    private val _calendar = MutableStateFlow<CalendarUiState>(CalendarUiState.PermissionRequired)
    val calendar: StateFlow<CalendarUiState> = _calendar.asStateFlow()

    private val _weather = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weather: StateFlow<WeatherUiState> = _weather.asStateFlow()

    init {
        loadInstalledApps()
        seedDefaultsIfNeeded()
        refreshAlarm()
        refreshCalendar()
        refreshWeather()
    }

    // ---- App grid actions ----

    private fun loadInstalledApps() = viewModelScope.launch {
        _installedApps.value = installedRepo.loadAll()
    }

    fun refreshInstalledApps() = loadInstalledApps()

    private fun seedDefaultsIfNeeded() = viewModelScope.launch {
        if (pinnedRepo.isSeeded()) return@launch
        val count = settingsRepo.settings.first().appCount
        val defaults = installedRepo.suggestedDefaults(count)
        // Bottom-align defaults: pad the top with empties up to count.
        val slots: List<PinnedAppRef?> = List(count) { i ->
            val offset = count - defaults.size
            if (i >= offset) defaults[i - offset] else null
        }
        pinnedRepo.save(slots)
    }

    fun launchApp(ref: PinnedAppRef) {
        if (!installedRepo.launch(ref)) {
            // Activity gone (uninstalled/disabled): refresh so the label/launchable state updates.
            loadInstalledApps()
        }
    }

    fun assignApp(index: Int, ref: PinnedAppRef) = viewModelScope.launch {
        pinnedRepo.assign(index, ref)
    }

    fun removeApp(index: Int) = viewModelScope.launch {
        pinnedRepo.remove(index)
    }

    /** Apps not currently pinned — the pool the picker offers. */
    fun availableToAdd(): List<AppInfo> {
        val pinnedRefs = pinnedSlots.value.filterIsInstance<PinnedSlot.Filled>().map { it.ref }.toSet()
        return installedApps.value.filter { it.ref !in pinnedRefs }
    }

    // ---- Settings actions ----
    // Note: block bodies (returning Unit) on purpose — these are used as method references
    // where a Unit-returning function type is expected.

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    }

    fun setClockStyle(style: ClockStyle) {
        viewModelScope.launch { settingsRepo.setClockStyle(style) }
    }

    fun setWeatherLocation(location: String) {
        viewModelScope.launch {
            settingsRepo.setWeatherLocation(location)
            refreshWeather()
        }
    }

    fun setAppCount(count: Int) {
        viewModelScope.launch {
            settingsRepo.setAppCount(count)
            pinnedRepo.resize(count)
        }
    }

    fun setNotificationFilter(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setNotificationFilter(enabled) }
    }

    // ---- Notes actions ----

    fun appendNoteLine(type: LineType, text: String, format: LineFormat) {
        viewModelScope.launch { notesRepo.appendLine(type, text, format) }
    }

    fun toggleCheckbox(id: Long) {
        viewModelScope.launch { notesRepo.toggleCheckbox(id) }
    }

    fun clearNote() {
        viewModelScope.launch { notesRepo.clearAll() }
    }

    // ---- Widget refreshes ----

    fun refreshAlarm() {
        _alarm.value = AlarmUiState(alarmRepo.nextAlarmTriggerTime())
    }

    fun refreshCalendar() = viewModelScope.launch {
        if (!calendarRepo.hasPermission()) {
            _calendar.value = CalendarUiState.PermissionRequired
            return@launch
        }
        _calendar.value = CalendarUiState.Loading
        val next = calendarRepo.nextEvent()
        val rest = calendarRepo.restOfToday()
        _calendar.value = CalendarUiState.Ready(next, rest)
    }

    fun refreshWeather() = viewModelScope.launch {
        _weather.value = WeatherUiState.Loading
        val manual = settingsRepo.settings.first().weatherLocation
        _weather.value = when (val r = weatherRepo.load(manual)) {
            is WeatherResult.Success -> WeatherUiState.Ready(r.weather)
            WeatherResult.NoLocation -> WeatherUiState.NeedsLocationPermission
            WeatherResult.Offline -> WeatherUiState.Offline
            WeatherResult.Error -> WeatherUiState.Error
        }
    }

    /** Called when the screen resumes so time-sensitive widget data stays fresh. */
    fun onResume() {
        refreshAlarm()
        refreshCalendar()
    }

    companion object {
        /** Normalises a stored slot list to exactly [count] slots (bottom-aligned). */
        fun resolveSlots(
            stored: List<PinnedAppRef?>,
            count: Int,
            apps: List<AppInfo>,
        ): List<PinnedSlot> {
            val normalized: List<PinnedAppRef?> = when {
                stored.size == count -> stored
                stored.size > count -> stored.takeLast(count)
                else -> List(count - stored.size) { null } + stored
            }
            val byRef = apps.associateBy { it.ref }
            return normalized.mapIndexed { index, ref ->
                if (ref == null) {
                    PinnedSlot.Empty(index)
                } else {
                    val info = byRef[ref]
                    PinnedSlot.Filled(
                        index = index,
                        ref = ref,
                        label = info?.label ?: ref.packageName.substringAfterLast('.'),
                        launchable = info != null,
                    )
                }
            }
        }
    }
}
