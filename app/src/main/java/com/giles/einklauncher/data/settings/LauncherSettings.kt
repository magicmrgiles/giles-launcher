package com.giles.einklauncher.data.settings

import com.giles.einklauncher.ui.theme.ThemeMode

/** How the clock renders the hour. */
enum class ClockStyle { TWELVE_HOUR, TWENTY_FOUR_HOUR }

/** Immutable snapshot of every user preference. */
data class LauncherSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** null = follow the device's own 12/24h setting; non-null = explicit override. */
    val clockStyle: ClockStyle? = null,
    /** Blank = use device location for weather; otherwise a manually typed place. */
    val weatherLocation: String = "",
    /** 4 or 8. */
    val appCount: Int = 8,
    /** When true, notifications from non-pinned packages are suppressed. */
    val notificationFilterEnabled: Boolean = false,
)
