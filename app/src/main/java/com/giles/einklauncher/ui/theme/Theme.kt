package com.giles.einklauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/** The three user-selectable theme modes. */
enum class ThemeMode { LIGHT, DARK, SYSTEM }

/**
 * Resolves [ThemeMode] against the live system setting. Because this reads
 * [isSystemInDarkTheme] inside composition, SYSTEM mode reacts immediately when the OS
 * theme flips while the app is open.
 */
@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun EinkTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val dark = themeMode.isDark()
    val colors = if (dark) DarkColors else LightColors

    // A Material3 scheme mapped onto the monochrome palette, so any stray Material
    // component still lands on pure black/white rather than a purple default.
    val material = if (dark) {
        darkColorScheme(
            primary = White, onPrimary = Black,
            background = Black, onBackground = White,
            surface = Black, onSurface = White,
            surfaceVariant = Black, onSurfaceVariant = White,
            outline = White,
        )
    } else {
        lightColorScheme(
            primary = Black, onPrimary = White,
            background = White, onBackground = Black,
            surface = White, onSurface = Black,
            surfaceVariant = White, onSurfaceVariant = Black,
            outline = Black,
        )
    }

    CompositionLocalProvider(LocalEinkColors provides colors) {
        MaterialTheme(
            colorScheme = material,
            typography = Typography(),
            content = content,
        )
    }
}

/** Convenience accessor: `EinkColorsProvider.current`. */
object EinkColorsProvider {
    val current: EinkColors
        @Composable @ReadOnlyComposable get() = LocalEinkColors.current
}

/** Short alias used throughout the UI. */
val einkColors: EinkColors
    @Composable @ReadOnlyComposable get() = LocalEinkColors.current

@Composable
@ReadOnlyComposable
fun contentColor(): Color = LocalEinkColors.current.content

@Composable
@ReadOnlyComposable
fun backgroundColor(): Color = LocalEinkColors.current.background
